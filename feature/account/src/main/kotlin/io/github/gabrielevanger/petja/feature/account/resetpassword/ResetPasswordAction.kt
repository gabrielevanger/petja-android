package io.github.gabrielevanger.petja.feature.account.resetpassword

import io.github.gabrielevanger.petja.feature.account.form.Field

/** Tudo o que a tela de senha nova pede ao ViewModel. */
sealed interface ResetPasswordAction {
    data class OnFieldChange(val field: Field, val value: String) : ResetPasswordAction

    data object OnSubmitClick : ResetPasswordAction

    data object OnBackClick : ResetPasswordAction
}
