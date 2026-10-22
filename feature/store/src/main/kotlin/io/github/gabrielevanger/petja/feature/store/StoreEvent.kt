package io.github.gabrielevanger.petja.feature.store

import androidx.compose.runtime.Immutable

/** O que acontece uma vez só: navegar ou mostrar um aviso curto. */
sealed interface StoreEvent {
    data object Close : StoreEvent

    data object OpenCart : StoreEvent

    data class ShowMessage(val message: StoreCartMessage) : StoreEvent
}

/** Aviso curto depois de tentar adicionar ao carrinho. */
@Immutable
sealed interface StoreCartMessage {
    data class Added(val productName: String, val variantLabel: String) : StoreCartMessage

    data object LimitReached : StoreCartMessage

    data object Unavailable : StoreCartMessage
}
