package io.github.gabrielevanger.petja.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gabrielevanger.petja.core.domain.account.AuthLinkHandler
import io.github.gabrielevanger.petja.core.domain.account.AuthLinkResult
import io.github.gabrielevanger.petja.feature.account.AccountNotice
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Para onde o app vai depois de abrir um link de e-mail da conta. */
sealed interface AppNavigation {
    data object ResetPassword : AppNavigation

    data class Account(val notice: AccountNotice) : AppNavigation
}

@HiltViewModel
class AppViewModel @Inject constructor(private val authLinks: AuthLinkHandler) : ViewModel() {

    private val _navigation = Channel<AppNavigation>(Channel.BUFFERED)
    val navigation: Flow<AppNavigation> = _navigation.receiveAsFlow()

    /** Links que abriram o app (petja://auth/...). Outros links são ignorados. */
    fun onLink(link: String?) {
        if (link == null) return
        viewModelScope.launch {
            val destination = when (authLinks.handle(link)) {
                AuthLinkResult.PasswordRecovery -> AppNavigation.ResetPassword
                AuthLinkResult.EmailConfirmed -> AppNavigation.Account(AccountNotice.EmailConfirmed)
                AuthLinkResult.Expired -> AppNavigation.Account(AccountNotice.LinkExpired)
                AuthLinkResult.NotAuthLink -> null
            }
            destination?.let { _navigation.send(it) }
        }
    }
}
