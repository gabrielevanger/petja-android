package io.github.gabrielevanger.petja.feature.account.signin

/** O que acontece uma vez só: navegação. */
sealed interface SignInEvent {
    data object Close : SignInEvent

    /** Entrou: volta para quem pediu o login (a conta ou o carrinho). */
    data object SignedIn : SignInEvent

    data object OpenForgotPassword : SignInEvent

    data object OpenSignUp : SignInEvent
}
