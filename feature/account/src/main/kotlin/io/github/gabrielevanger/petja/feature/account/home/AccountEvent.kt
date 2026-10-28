package io.github.gabrielevanger.petja.feature.account.home

/** O que acontece uma vez só: navegação. */
sealed interface AccountEvent {
    data object Close : AccountEvent

    data object OpenSignIn : AccountEvent

    data object OpenSignUp : AccountEvent

    data object OpenOrders : AccountEvent

    data object OpenStoreMode : AccountEvent
}
