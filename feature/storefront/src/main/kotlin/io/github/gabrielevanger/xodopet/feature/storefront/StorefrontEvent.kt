package io.github.gabrielevanger.xodopet.feature.storefront

/** O que acontece uma vez só e a tela executa: hoje, só navegação. */
sealed interface StorefrontEvent {
    data class OpenStore(val storeId: String) : StorefrontEvent

    data object OpenCart : StorefrontEvent

    data object OpenAccount : StorefrontEvent
}
