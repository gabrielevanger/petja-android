package io.github.gabrielevanger.petja.feature.cart

/** O que acontece uma vez só: navegação. */
sealed interface CartEvent {
    data object Close : CartEvent

    data object OpenCheckout : CartEvent

    /** Entrar e voltar para o carrinho. */
    data object OpenSignIn : CartEvent
}
