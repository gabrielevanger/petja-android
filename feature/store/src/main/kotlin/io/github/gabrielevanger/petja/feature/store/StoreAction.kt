package io.github.gabrielevanger.petja.feature.store

/** Tudo o que a página da loja pede ao ViewModel. */
sealed interface StoreAction {
    /** A tela ficou visível: na primeira vez carrega o catálogo, e sempre volta a ouvir o carrinho. */
    data object OnStart : StoreAction

    /** A tela saiu de vista: para de ouvir o carrinho. */
    data object OnStop : StoreAction

    data object OnBackClick : StoreAction

    data object OnRetryClick : StoreAction

    data class OnProductClick(val productId: String) : StoreAction

    data object OnProductDismiss : StoreAction

    /** Adiciona uma unidade da opção escolhida no detalhe do produto. */
    data class OnAddToCart(val variantId: String) : StoreAction

    data object OnConfirmReplaceCart : StoreAction

    data object OnDismissReplaceCart : StoreAction

    data object OnCartClick : StoreAction
}
