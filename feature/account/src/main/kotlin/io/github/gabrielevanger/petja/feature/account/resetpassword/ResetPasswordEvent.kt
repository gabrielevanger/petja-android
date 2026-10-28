package io.github.gabrielevanger.petja.feature.account.resetpassword

/** O que acontece uma vez só: navegação. */
sealed interface ResetPasswordEvent {
    data object Close : ResetPasswordEvent

    /** Senha trocada: a conta abre com o aviso de senha alterada. */
    data object Done : ResetPasswordEvent
}
