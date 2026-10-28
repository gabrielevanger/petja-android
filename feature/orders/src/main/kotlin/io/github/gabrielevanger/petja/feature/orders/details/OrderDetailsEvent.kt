package io.github.gabrielevanger.petja.feature.orders.details

/** O que acontece uma vez só: navegar, abrir um link ou mostrar um aviso curto. */
sealed interface OrderDetailsEvent {
    data object Close : OrderDetailsEvent

    /** Depois de "Pedir de novo" com algum item no carrinho. */
    data object OpenCart : OrderDetailsEvent

    data class OpenLink(val uri: String) : OrderDetailsEvent

    data class ShowMessage(val message: OrderMessage) : OrderDetailsEvent
}

/** Aviso curto depois de uma ação. */
enum class OrderMessage { CannotCancel, ReorderSomeChanged, ReorderNothing, ActionFailed }
