package io.github.gabrielevanger.petja.feature.orders.list

/** Tudo o que a lista de pedidos pede ao ViewModel. */
sealed interface OrdersAction {
    /** A lista ficou visível: carrega, e recarrega toda vez que volta (status podem ter mudado). */
    data object OnStart : OrdersAction

    data object OnBackClick : OrdersAction

    data object OnRetryClick : OrdersAction

    data object OnSignInClick : OrdersAction

    data class OnOrderClick(val orderId: String) : OrdersAction
}
