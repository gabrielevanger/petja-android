package io.github.gabrielevanger.petja.feature.checkout

/** O que acontece uma vez só: navegação. */
sealed interface CheckoutEvent {
    data object Close : CheckoutEvent

    data object OpenSignIn : CheckoutEvent

    /** O pedido foi aceito pelo servidor: abre a confirmação. */
    data class OrderPlaced(val order: PlacedOrderUi) : CheckoutEvent
}
