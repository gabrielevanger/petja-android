package io.github.gabrielevanger.petja.feature.storemode

import io.github.gabrielevanger.petja.core.domain.storemode.StoreAction
import io.github.gabrielevanger.petja.core.domain.storemode.StoreOrder

/** Tudo o que o modo lojista pede ao ViewModel. As etapas do pedido em si são o [StoreAction] do domínio. */
sealed interface StoreModeAction {
    /** A tela ficou visível: na primeira vez carrega as lojas; sempre recarrega e liga o Realtime. */
    data object OnStart : StoreModeAction

    /** A tela saiu de vista: desliga o Realtime. */
    data object OnStop : StoreModeAction

    data object OnBackClick : StoreModeAction

    data object OnSignInClick : StoreModeAction

    data object OnRetryClick : StoreModeAction

    data class OnStoreClick(val storeId: String) : StoreModeAction

    /** Botão de etapa de um pedido (aceitar, separar, saiu…). Recusa e não entrega pedem o motivo antes. */
    data class OnOrderAction(val order: StoreOrder, val action: StoreAction) : StoreModeAction

    data class OnConfirmReason(val reason: String) : StoreModeAction

    data object OnDismissReason : StoreModeAction
}
