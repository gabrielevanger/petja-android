package io.github.gabrielevanger.petja.feature.account.signup

/** O que acontece uma vez só: navegação. */
sealed interface SignUpEvent {
    data object Close : SignUpEvent

    /** Cadastro sem confirmação por e-mail: já entrou. */
    data object SignedIn : SignUpEvent
}
