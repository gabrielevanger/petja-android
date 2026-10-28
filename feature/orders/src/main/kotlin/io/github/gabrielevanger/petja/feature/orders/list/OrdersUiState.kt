package io.github.gabrielevanger.petja.feature.orders.list

import androidx.compose.runtime.Immutable
import io.github.gabrielevanger.petja.core.domain.model.OrderSummary
import kotlinx.collections.immutable.ImmutableList

@Immutable
sealed interface OrdersUiState {
    data object Loading : OrdersUiState

    data object Error : OrdersUiState

    data object NotSignedIn : OrdersUiState

    data class Loaded(val orders: ImmutableList<OrderSummary>) : OrdersUiState
}
