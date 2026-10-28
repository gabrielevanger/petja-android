package io.github.gabrielevanger.petja.feature.orders.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gabrielevanger.petja.core.domain.DiagnosticLog
import io.github.gabrielevanger.petja.core.domain.orders.OrderError
import io.github.gabrielevanger.petja.core.domain.orders.OrderException
import io.github.gabrielevanger.petja.core.domain.orders.OrderRepository
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

@HiltViewModel
class OrdersViewModel @Inject constructor(private val orders: OrderRepository, private val log: DiagnosticLog) :
    ViewModel() {

    private val _uiState = MutableStateFlow<OrdersUiState>(OrdersUiState.Loading)
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    private val _events = Channel<OrdersEvent>(Channel.BUFFERED)
    val events: Flow<OrdersEvent> = _events.receiveAsFlow()

    private var loading: Job? = null

    fun onAction(action: OrdersAction) {
        when (action) {
            OrdersAction.OnStart, OrdersAction.OnRetryClick -> load()
            OrdersAction.OnBackClick -> send(OrdersEvent.Close)
            OrdersAction.OnSignInClick -> send(OrdersEvent.OpenSignIn)
            is OrdersAction.OnOrderClick -> send(OrdersEvent.OpenOrder(action.orderId))
        }
    }

    /** Ao recarregar, a lista que já está na tela fica até a resposta chegar (sem piscar o carregando). */
    private fun load() {
        loading?.cancel()
        loading = flow { emit(orders.myOrders()) }
            .onEach { _uiState.value = OrdersUiState.Loaded(it.toImmutableList()) }
            .catch { error ->
                _uiState.value = if (error is OrderException && error.error == OrderError.NotSignedIn) {
                    OrdersUiState.NotSignedIn
                } else {
                    log.warn("Falha ao carregar os pedidos", error)
                    OrdersUiState.Error
                }
            }
            .launchIn(viewModelScope)
    }

    private fun send(event: OrdersEvent) {
        _events.trySend(event)
    }
}
