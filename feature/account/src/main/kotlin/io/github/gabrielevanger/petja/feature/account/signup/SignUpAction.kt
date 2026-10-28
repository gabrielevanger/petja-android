package io.github.gabrielevanger.petja.feature.account.signup

import io.github.gabrielevanger.petja.feature.account.form.Field

/** Tudo o que a tela de criar conta pede ao ViewModel. */
sealed interface SignUpAction {
    data class OnFieldChange(val field: Field, val value: String) : SignUpAction

    data object OnSubmitClick : SignUpAction

    data object OnBackClick : SignUpAction
}
