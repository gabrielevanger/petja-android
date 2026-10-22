package io.github.gabrielevanger.petja.feature.orders

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
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OrdersViewModel @Inject constructor(private val orders: OrderRepository, private val log: DiagnosticLog) :
    ViewModel() {

    private val _uiState = MutableStateFlow<OrdersUiState>(OrdersUiState.Loading)
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    /** Chamado sempre que a lista aparece (inclusive ao voltar dos detalhes), para os status estarem em dia. */
    fun load() {
        viewModelScope.launch {
            _uiState.value = try {
                OrdersUiState.Loaded(orders.myOrders().toImmutableList())
            } catch (e: OrderException) {
                if (e.error == OrderError.NotSignedIn) {
                    OrdersUiState.NotSignedIn
                } else {
                    log.warn("Falha ao carregar os pedidos", e)
                    OrdersUiState.Error
                }
            }
        }
    }
}

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

    private val _openCart = Channel<Unit>(Channel.BUFFERED)

    /** Depois de "Pedir de novo" com algum item no carrinho, a tela abre o carrinho. */
    val openCart: Flow<Unit> = _openCart.receiveAsFlow()

    init {
        viewModelScope.launch { reload() }
        listenToChanges()
    }

    fun retry() {
        _uiState.value = OrderDetailsUiState.Loading
        viewModelScope.launch { reload() }
    }

    fun askToCancel() = updateLoaded { it.copy(confirmingCancel = true) }

    fun dismissCancel() = updateLoaded { it.copy(confirmingCancel = false) }

    fun confirmCancel() {
        updateLoaded { it.copy(confirmingCancel = false, working = true) }
        viewModelScope.launch {
            val message = try {
                orders.cancel(orderId)
                null
            } catch (e: OrderException) {
                if (e.error == OrderError.CannotCancel) {
                    OrderMessage.CannotCancel
                } else {
                    log.warn("Falha ao cancelar o pedido $orderId", e)
                    OrderMessage.ActionFailed
                }
            }
            reload()
            updateLoaded { it.copy(working = false, message = message) }
        }
    }

    fun reorder() {
        val order = (_uiState.value as? OrderDetailsUiState.Loaded)?.order ?: return
        updateLoaded { it.copy(working = true) }
        viewModelScope.launch {
            val message = try {
                val result = reorderToCart(order)
                if (result != ReorderResult.NothingAvailable) _openCart.send(Unit)
                when (result) {
                    ReorderResult.AllAdded -> null
                    is ReorderResult.SomeChanged -> OrderMessage.ReorderSomeChanged
                    ReorderResult.NothingAvailable -> OrderMessage.ReorderNothing
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.warn("Falha ao pedir de novo o pedido $orderId", e)
                OrderMessage.ActionFailed
            }
            updateLoaded { it.copy(working = false, message = message) }
        }
    }

    fun messageShown() = updateLoaded { it.copy(message = null) }

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

    /** Cada mudança no pedido recarrega os detalhes; sem Realtime, a tela continua funcionando. */
    private fun listenToChanges() {
        viewModelScope.launch {
            try {
                orders.changes(orderId).collect { reload() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.warn("Status ao vivo indisponível para o pedido $orderId", e)
            }
        }
    }

    private fun updateLoaded(change: (OrderDetailsUiState.Loaded) -> OrderDetailsUiState.Loaded) =
        _uiState.update { state ->
            if (state is OrderDetailsUiState.Loaded) change(state) else state
        }
}
