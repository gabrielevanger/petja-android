package io.github.gabrielevanger.xodopet.feature.orders

import androidx.compose.runtime.Immutable
import io.github.gabrielevanger.xodopet.core.domain.model.OrderDetails
import io.github.gabrielevanger.xodopet.core.domain.model.OrderSummary
import kotlinx.collections.immutable.ImmutableList

@Immutable
sealed interface OrdersUiState {
    data object Loading : OrdersUiState

    data object Error : OrdersUiState

    data object NotSignedIn : OrdersUiState

    data class Loaded(val orders: ImmutableList<OrderSummary>) : OrdersUiState
}

@Immutable
sealed interface OrderDetailsUiState {
    data object Loading : OrderDetailsUiState

    data object Error : OrderDetailsUiState

    data object NotFound : OrderDetailsUiState

    data class Loaded(
        val order: OrderDetails,
        val confirmingCancel: Boolean = false,
        val working: Boolean = false,
        val message: OrderMessage? = null,
    ) : OrderDetailsUiState
}

/** Aviso curto depois de uma ação, mostrado uma vez. */
enum class OrderMessage { CannotCancel, ReorderSomeChanged, ReorderNothing, ActionFailed }
