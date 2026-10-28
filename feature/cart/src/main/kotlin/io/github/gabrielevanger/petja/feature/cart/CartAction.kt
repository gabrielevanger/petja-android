package io.github.gabrielevanger.petja.feature.cart

/** Tudo o que o carrinho pede ao ViewModel. */
sealed interface CartAction {
    data object OnBackClick : CartAction

    data class OnIncrease(val variantId: String, val quantity: Int) : CartAction

    /** Com uma unidade, tira o item do carrinho. */
    data class OnDecrease(val variantId: String, val quantity: Int) : CartAction

    data object OnClearClick : CartAction

    /** Segue para o checkout, ou para entrar quando não há login. */
    data object OnContinueClick : CartAction
}
