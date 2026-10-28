package io.github.gabrielevanger.petja.feature.orders.details

import androidx.compose.runtime.Immutable
import io.github.gabrielevanger.petja.core.domain.model.OrderDetails

@Immutable
sealed interface OrderDetailsUiState {
    data object Loading : OrderDetailsUiState

    data object Error : OrderDetailsUiState

    data object NotFound : OrderDetailsUiState

    /**
     * @property confirmingCancel a pergunta "Cancelar o pedido?" está aberta.
     * @property working cancelamento ou "pedir de novo" a caminho; os botões ficam travados.
     */
    data class Loaded(val order: OrderDetails, val confirmingCancel: Boolean = false, val working: Boolean = false) :
        OrderDetailsUiState
}
