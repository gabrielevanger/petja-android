package io.github.gabrielevanger.petja.feature.account.forgotpassword

/** O que acontece uma vez só: navegação. */
sealed interface ForgotPasswordEvent {
    data object Close : ForgotPasswordEvent
}
