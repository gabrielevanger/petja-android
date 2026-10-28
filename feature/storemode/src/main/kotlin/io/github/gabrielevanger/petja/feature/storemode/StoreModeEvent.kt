package io.github.gabrielevanger.petja.feature.storemode

/** O que acontece uma vez só: navegar ou mostrar um aviso curto. */
sealed interface StoreModeEvent {
    data object Close : StoreModeEvent

    data object OpenSignIn : StoreModeEvent

    data class ShowMessage(val message: StoreMessage) : StoreModeEvent
}

/** Aviso curto depois de uma mudança de etapa ou de uma recarga que falhou. */
enum class StoreMessage { ActionFailed, OutOfDate, NotAllowed }
