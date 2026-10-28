package io.github.gabrielevanger.petja.feature.account.forgotpassword

import io.github.gabrielevanger.petja.feature.account.form.Field

/** Tudo o que a tela de esqueci a senha pede ao ViewModel. */
sealed interface ForgotPasswordAction {
    data class OnFieldChange(val field: Field, val value: String) : ForgotPasswordAction

    data object OnSubmitClick : ForgotPasswordAction

    data object OnBackClick : ForgotPasswordAction
}
