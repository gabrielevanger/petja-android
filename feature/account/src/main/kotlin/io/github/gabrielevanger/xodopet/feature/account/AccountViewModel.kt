package io.github.gabrielevanger.xodopet.feature.account

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gabrielevanger.xodopet.core.domain.DiagnosticLog
import io.github.gabrielevanger.xodopet.core.domain.account.AccountException
import io.github.gabrielevanger.xodopet.core.domain.account.AccountRepository
import io.github.gabrielevanger.xodopet.core.domain.account.AccountState
import io.github.gabrielevanger.xodopet.core.domain.storemode.StoreOrderException
import io.github.gabrielevanger.xodopet.core.domain.storemode.StoreOrderRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AccountViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accounts: AccountRepository,
    private val storeOrders: StoreOrderRepository,
    private val log: DiagnosticLog,
) : ViewModel() {

    val notice: AccountNotice? =
        savedStateHandle.get<String>(NOTICE_ARG)?.let { name -> AccountNotice.entries.firstOrNull { it.name == name } }

    val state: StateFlow<AccountState> = accounts.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AccountState.Loading)

    /** Quem é dono ou atendente de uma loja ganha o atalho para o modo lojista (RF-80). */
    @OptIn(ExperimentalCoroutinesApi::class)
    val canOperateStore: StateFlow<Boolean> = accounts.state
        .mapLatest { state ->
            state is AccountState.SignedIn && try {
                storeOrders.managedStores().isNotEmpty()
            } catch (e: StoreOrderException) {
                log.warn("Falha ao conferir as lojas da conta", e)
                false
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    fun signOut() {
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
