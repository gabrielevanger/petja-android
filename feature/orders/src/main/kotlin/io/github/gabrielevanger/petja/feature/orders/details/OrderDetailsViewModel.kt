package io.github.gabrielevanger.petja.feature.orders.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gabrielevanger.petja.core.domain.DiagnosticLog
import io.github.gabrielevanger.petja.core.domain.orders.OrderError
import io.github.gabrielevanger.petja.core.domain.orders.OrderException
import io.github.gabrielevanger.petja.core.domain.orders.OrderRepository
import io.github.gabrielevanger.petja.core.domain.orders.ReorderResult
import io.github.gabrielevanger.petja.core.domain.orders.ReorderToCart
import io.github.gabrielevanger.petja.core.ui.whatsappLink
import io.github.gabrielevanger.petja.feature.orders.ORDER_ID_ARG
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OrderDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orders: OrderRepository,
    private val reorderToCart: ReorderToCart,
    private val log: DiagnosticLog,
) : ViewModel() {

    private val orderId: String = requireNotNull(savedStateHandle[ORDER_ID_ARG]) { "Rota do pedido sem $ORDER_ID_ARG" }

    private val _uiState = MutableStateFlow<OrderDetailsUiState>(OrderDetailsUiState.Loading)
    val uiState: StateFlow<OrderDetailsUiState> = _uiState.asStateFlow()

    private val _events = Channel<OrderDetailsEvent>(Channel.BUFFERED)
    val events: Flow<OrderDetailsEvent> = _events.receiveAsFlow()

    private var listening: Job? = null

    fun onAction(action: OrderDetailsAction) {
        when (action) {
            OrderDetailsAction.OnStart -> onStart()

            OrderDetailsAction.OnStop -> listening?.cancel()

            OrderDetailsAction.OnBackClick -> send(OrderDetailsEvent.Close)

            OrderDetailsAction.OnRetryClick -> {
                _uiState.value = OrderDetailsUiState.Loading
                viewModelScope.launch { reload() }
            }

            OrderDetailsAction.OnCancelClick -> updateLoaded { it.copy(confirmingCancel = true) }

            OrderDetailsAction.OnDismissCancel -> updateLoaded { it.copy(confirmingCancel = false) }

            OrderDetailsAction.OnConfirmCancel -> confirmCancel()

            OrderDetailsAction.OnReorderClick -> reorder()

            is OrderDetailsAction.OnWhatsappClick -> send(OrderDetailsEvent.OpenLink(whatsappLink(action.number)))
        }
    }

    /**
     * Na primeira vez carrega o pedido; nas outras recarrega, porque o status pode ter mudado com a tela
     * fora de vista. Em todas, liga o status ao vivo, que o OnStop desliga.
     */
    private fun onStart() {
        viewModelScope.launch { reload() }
        listening?.cancel()
        listening = orders.changes(orderId)
            .onEach { reload() }
            .catch { log.warn("Status ao vivo indisponível para o pedido $orderId", it) }
            .launchIn(viewModelScope)
    }

    private fun confirmCancel() {
        updateLoaded { it.copy(confirmingCancel = false, working = true) }
        viewModelScope.launch {
            try {
                orders.cancel(orderId)
            } catch (e: OrderException) {
                if (e.error == OrderError.CannotCancel) {
                    send(OrderDetailsEvent.ShowMessage(OrderMessage.CannotCancel))
                } else {
                    log.warn("Falha ao cancelar o pedido $orderId", e)
                    send(OrderDetailsEvent.ShowMessage(OrderMessage.ActionFailed))
                }
            }
            reload()
            updateLoaded { it.copy(working = false) }
        }
    }

    private fun reorder() {
        val order = (_uiState.value as? OrderDetailsUiState.Loaded)?.order ?: return
        updateLoaded { it.copy(working = true) }
        viewModelScope.launch {
            try {
                val result = reorderToCart(order)
                if (result != ReorderResult.NothingAvailable) send(OrderDetailsEvent.OpenCart)
                when (result) {
                    ReorderResult.AllAdded -> Unit
                    is ReorderResult.SomeChanged -> send(OrderDetailsEvent.ShowMessage(OrderMessage.ReorderSomeChanged))
                    ReorderResult.NothingAvailable -> send(OrderDetailsEvent.ShowMessage(OrderMessage.ReorderNothing))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.warn("Falha ao pedir de novo o pedido $orderId", e)
                send(OrderDetailsEvent.ShowMessage(OrderMessage.ActionFailed))
            }
            updateLoaded { it.copy(working = false) }
        }
    }

    private suspend fun reload() {
        val result = try {
            orders.order(orderId)?.let { order ->
                val previous = _uiState.value as? OrderDetailsUiState.Loaded
                previous?.copy(order = order) ?: OrderDetailsUiState.Loaded(order)
            } ?: OrderDetailsUiState.NotFound
        } catch (e: OrderException) {
            log.warn("Falha ao carregar o pedido $orderId", e)
            // Com a tela já aberta, uma falha ao atualizar mantém o que está na tela.
            _uiState.value as? OrderDetailsUiState.Loaded ?: OrderDetailsUiState.Error
        }
        _uiState.value = result
    }

    private fun send(event: OrderDetailsEvent) {
        _events.trySend(event)
    }

    private fun updateLoaded(change: (OrderDetailsUiState.Loaded) -> OrderDetailsUiState.Loaded) =
        _uiState.update { state ->
            if (state is OrderDetailsUiState.Loaded) change(state) else state
        }
}
