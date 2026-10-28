package io.github.gabrielevanger.petja.feature.account.home

/** Tudo o que a tela da conta pede ao ViewModel. */
sealed interface AccountAction {
    data object OnBackClick : AccountAction

    data object OnSignInClick : AccountAction

    data object OnSignUpClick : AccountAction

    data object OnOrdersClick : AccountAction

    data object OnStoreModeClick : AccountAction

    data object OnSignOutClick : AccountAction
}
