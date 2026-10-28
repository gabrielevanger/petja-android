package io.github.gabrielevanger.petja.feature.account.signin

import io.github.gabrielevanger.petja.feature.account.form.Field

/** Tudo o que a tela de entrar pede ao ViewModel. */
sealed interface SignInAction {
    data class OnFieldChange(val field: Field, val value: String) : SignInAction

    data object OnSubmitClick : SignInAction

    data object OnBackClick : SignInAction

    data object OnForgotPasswordClick : SignInAction

    data object OnSignUpClick : SignInAction
}
