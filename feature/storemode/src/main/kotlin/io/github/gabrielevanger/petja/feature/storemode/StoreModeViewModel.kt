package io.github.gabrielevanger.petja.feature.storemode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gabrielevanger.petja.core.domain.DiagnosticLog
import io.github.gabrielevanger.petja.core.domain.storemode.ManagedStore
import io.github.gabrielevanger.petja.core.domain.storemode.StoreAction
import io.github.gabrielevanger.petja.core.domain.storemode.StoreOrder
import io.github.gabrielevanger.petja.core.domain.storemode.StoreOrderError
import io.github.gabrielevanger.petja.core.domain.storemode.StoreOrderException
import io.github.gabrielevanger.petja.core.domain.storemode.StoreOrderRepository
import io.github.gabrielevanger.petja.core.domain.storemode.newOrders
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Pedidos da loja ao vivo: carrega, escuta o Realtime da loja e recarrega a cada mudança. Pedido
 * novo toca o alerta. As mudanças de etapa vão pela função do servidor.
 */
@HiltViewModel
class StoreModeViewModel @Inject constructor(
    private val repository: StoreOrderRepository,
    private val alert: NewOrderAlert,
    private val log: DiagnosticLog,
) : ViewModel() {

    private val _uiState = MutableStateFlow<StoreModeUiState>(StoreModeUiState.Loading)
    val uiState: StateFlow<StoreModeUiState> = _uiState.asStateFlow()

    private var listening: Job? = null

    /** Última lista conhecida, para descobrir quais pedidos são novos; nula antes da primeira carga. */
    private var lastOrders: List<StoreOrder>? = null

    init {
        start()
    }

    fun retry() = start()

    fun selectStore(storeId: String) {
        val state = _uiState.value as? StoreModeUiState.Loaded ?: return
        if (state.storeId == storeId) return
        open(state.stores, storeId)
    }

    fun act(order: StoreOrder, action: StoreAction) {
        if (action.needsReason) {
            updateLoaded { it.copy(askingReason = ReasonRequest(order.id, order.number, action)) }
        } else {
            transition(order.id, action, reason = null)
        }
    }

    fun confirmReason(reason: String) {
        val request = (_uiState.value as? StoreModeUiState.Loaded)?.askingReason ?: return
        if (reason.isBlank()) return
        updateLoaded { it.copy(askingReason = null) }
        transition(request.orderId, request.action, reason.trim())
    }

    fun dismissReason() = updateLoaded { it.copy(askingReason = null) }

    fun messageShown() = updateLoaded { it.copy(message = null) }

    private fun start() {
        _uiState.value = StoreModeUiState.Loading
        viewModelScope.launch {
            try {
                val stores = repository.managedStores()
                if (stores.isEmpty()) {
                    _uiState.value = StoreModeUiState.NoStore
                } else {
                    open(stores.toImmutableList(), stores.first().id)
                }
            } catch (e: StoreOrderException) {
                fail(e)
            }
        }
    }

    /** Troca de loja: zera a lista conhecida (não apita pelos pedidos da outra) e escuta só a nova. */
    private fun open(stores: ImmutableList<ManagedStore>, storeId: String) {
        listening?.cancel()
        lastOrders = null
        _uiState.value = StoreModeUiState.Loading
        listening = viewModelScope.launch {
            reload(stores, storeId)
            try {
                repository.changes(storeId).collect { reload(stores, storeId) }
            } catch (e: StoreOrderException) {
                log.warn("Realtime da loja caiu", e)
            }
        }
    }

    private suspend fun reload(stores: ImmutableList<ManagedStore>, storeId: String) {
        try {
            val orders = repository.orders(storeId)
            if (newOrders(lastOrders, orders).isNotEmpty()) alert.play()
            lastOrders = orders
            _uiState.update { current ->
                val loaded = current as? StoreModeUiState.Loaded
                loaded?.takeIf { it.storeId == storeId }?.copy(orders = orders.toImmutableList())
                    ?: StoreModeUiState.Loaded(stores, storeId, orders.toImmutableList())
            }
        } catch (e: StoreOrderException) {
            // Com a lista já na tela, uma recarga que falha não apaga o que a loja está vendo.
            if (_uiState.value is StoreModeUiState.Loaded) {
                log.warn("Falha ao atualizar os pedidos da loja", e)
                updateLoaded { it.copy(message = StoreMessage.ActionFailed) }
            } else {
                fail(e)
            }
        }
    }

    private fun transition(orderId: String, action: StoreAction, reason: String?) {
        val state = _uiState.value as? StoreModeUiState.Loaded ?: return
        updateLoaded { it.copy(working = orderId) }
        viewModelScope.launch {
            try {
                repository.transition(orderId, action.to, reason)
            } catch (e: StoreOrderException) {
                val message = when (e.error) {
                    StoreOrderError.InvalidTransition -> StoreMessage.OutOfDate

                    StoreOrderError.NotAllowed, StoreOrderError.NotSignedIn -> StoreMessage.NotAllowed

                    StoreOrderError.ReasonRequired, StoreOrderError.Network, StoreOrderError.Unknown -> {
                        log.warn("Falha ao mudar a etapa do pedido", e)
                        StoreMessage.ActionFailed
                    }
                }
                updateLoaded { it.copy(message = message) }
            }
            // Recarrega mesmo depois de erro: se outra pessoa já mudou o pedido, a tela se acerta.
            reload(state.stores, state.storeId)
            updateLoaded { it.copy(working = null) }
        }
    }

    private fun fail(e: StoreOrderException) {
        _uiState.value = if (e.error == StoreOrderError.NotSignedIn) {
            StoreModeUiState.NotSignedIn
        } else {
            log.warn("Falha ao abrir o modo lojista", e)
            StoreModeUiState.Error
        }
    }

    private fun updateLoaded(change: (StoreModeUiState.Loaded) -> StoreModeUiState.Loaded) {
        _uiState.update { (it as? StoreModeUiState.Loaded)?.let(change) ?: it }
    }
}
