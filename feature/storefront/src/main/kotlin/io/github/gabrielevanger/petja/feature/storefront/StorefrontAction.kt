package io.github.gabrielevanger.petja.feature.storefront

/** Tudo o que a vitrine pede ao ViewModel; a tela não chama funções soltas. */
sealed interface StorefrontAction {
    /** A tela ficou visível: na primeira vez carrega a vitrine, e sempre volta a ouvir o carrinho. */
    data object OnStart : StorefrontAction

    /** A tela saiu de vista: para de ouvir o carrinho. */
    data object OnStop : StorefrontAction

    data class OnPlaceClick(val place: SamplePlace) : StorefrontAction

    data object OnRetryClick : StorefrontAction

    data class OnStoreClick(val storeId: String) : StorefrontAction

    data object OnCartClick : StorefrontAction

    data object OnAccountClick : StorefrontAction
}
