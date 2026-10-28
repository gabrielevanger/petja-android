package io.github.gabrielevanger.petja.feature.account.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gabrielevanger.petja.core.domain.DiagnosticLog
import io.github.gabrielevanger.petja.core.domain.account.AccountException
import io.github.gabrielevanger.petja.core.domain.account.AccountRepository
import io.github.gabrielevanger.petja.core.domain.account.AccountState
import io.github.gabrielevanger.petja.core.domain.storemode.StoreOrderException
import io.github.gabrielevanger.petja.core.domain.storemode.StoreOrderRepository
import io.github.gabrielevanger.petja.feature.account.AccountNotice
import io.github.gabrielevanger.petja.feature.account.NOTICE_ARG
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado derivado só de flows (a conta e as lojas que ela opera): o stateIn(WhileSubscribed) liga e
 * desliga com a tela, sem OnStart e OnStop.
 */
@HiltViewModel
class AccountViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accounts: AccountRepository,
    private val storeOrders: StoreOrderRepository,
    private val log: DiagnosticLog,
) : ViewModel() {

    private val notice: AccountNotice? =
        savedStateHandle.get<String>(NOTICE_ARG)?.let { name -> AccountNotice.entries.firstOrNull { it.name == name } }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AccountUiState> = accounts.state
        .mapLatest { account -> AccountUiState(account, notice, canOperateStore(account)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AccountUiState(notice = notice))

    private val _events = Channel<AccountEvent>(Channel.BUFFERED)
    val events: Flow<AccountEvent> = _events.receiveAsFlow()

    fun onAction(action: AccountAction) {
        when (action) {
            AccountAction.OnBackClick -> _events.trySend(AccountEvent.Close)
            AccountAction.OnSignInClick -> _events.trySend(AccountEvent.OpenSignIn)
            AccountAction.OnSignUpClick -> _events.trySend(AccountEvent.OpenSignUp)
            AccountAction.OnOrdersClick -> _events.trySend(AccountEvent.OpenOrders)
            AccountAction.OnStoreModeClick -> _events.trySend(AccountEvent.OpenStoreMode)
            AccountAction.OnSignOutClick -> signOut()
        }
    }

    /** Quem é dono ou atendente de uma loja ganha o atalho para o modo lojista (RF-80). */
    private suspend fun canOperateStore(account: AccountState): Boolean = account is AccountState.SignedIn && try {
        storeOrders.managedStores().isNotEmpty()
    } catch (e: StoreOrderException) {
        log.warn("Falha ao conferir as lojas da conta", e)
        false
    }

    private fun signOut() {
        viewModelScope.launch {
            try {
                accounts.signOut()
            } catch (e: AccountException) {
                // A sessão local sai mesmo se o servidor não responder; só registra.
                log.warn("Falha ao sair da conta", e)
            }
        }
    }
}

/** Mantém o estado vivo por alguns segundos sem tela, para sobreviver à rotação. */
private const val STOP_TIMEOUT_MILLIS = 5_000L
