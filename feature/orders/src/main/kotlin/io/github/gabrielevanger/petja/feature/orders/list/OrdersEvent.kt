package io.github.gabrielevanger.petja.feature.orders.list

/** O que acontece uma vez só: navegação. */
sealed interface OrdersEvent {
    data object Close : OrdersEvent

    data class OpenOrder(val orderId: String) : OrdersEvent

    data object OpenSignIn : OrdersEvent
}
