package io.github.gabrielevanger.xodopet.feature.store

import androidx.compose.runtime.Immutable

/** O que a página da loja mostra sobre o carrinho, fora do catálogo. */
@Immutable
data class StoreCartState(
    /** Pergunta antes de trocar o carrinho de outra loja por este (RF-41). */
    val replacePrompt: ReplaceCartPrompt? = null,
    /** Aviso curto depois de tentar adicionar, mostrado uma vez. */
    val message: StoreCartMessage? = null,
)

@Immutable
data class ReplaceCartPrompt(val currentStoreName: String, val variantId: String)

@Immutable
sealed interface StoreCartMessage {
    data class Added(val productName: String, val variantLabel: String) : StoreCartMessage

    data object LimitReached : StoreCartMessage

    data object Unavailable : StoreCartMessage
}
