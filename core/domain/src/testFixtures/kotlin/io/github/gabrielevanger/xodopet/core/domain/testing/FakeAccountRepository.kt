package io.github.gabrielevanger.xodopet.core.domain.testing

import io.github.gabrielevanger.xodopet.core.domain.account.Account
import io.github.gabrielevanger.xodopet.core.domain.account.AccountError
import io.github.gabrielevanger.xodopet.core.domain.account.AccountException
import io.github.gabrielevanger.xodopet.core.domain.account.AccountRepository
import io.github.gabrielevanger.xodopet.core.domain.account.AccountState
import io.github.gabrielevanger.xodopet.core.domain.account.SignUpResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

val ANA = Account(id = "ana", email = "ana@teste.local", fullName = "Ana Souza")

/** Contas em memória. `failWith` faz a próxima chamada falhar com o motivo dado. */
class FakeAccountRepository(initial: AccountState = AccountState.SignedOut) : AccountRepository {
    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<AccountState> = _state

    var failWith: AccountError? = null
    var signUpResult = SignUpResult.SignedIn
    val passwordResets = mutableListOf<String>()
    var lastPassword: String? = null

    private fun failIfAsked() {
        failWith?.let {
            failWith = null
            throw AccountException(it)
        }
    }

    override suspend fun signIn(email: String, password: String) {
        failIfAsked()
        _state.value = AccountState.SignedIn(ANA.copy(email = email))
    }

    override suspend fun signUp(fullName: String, email: String, password: String): SignUpResult {
        failIfAsked()
        if (signUpResult == SignUpResult.SignedIn) {
            _state.value = AccountState.SignedIn(Account("nova", email, fullName))
        }
        return signUpResult
    }

    override suspend fun sendPasswordReset(email: String) {
        failIfAsked()
        passwordResets += email
    }

    override suspend fun updatePassword(newPassword: String) {
        failIfAsked()
        lastPassword = newPassword
    }

    override suspend fun signOut() {
        failIfAsked()
        _state.value = AccountState.SignedOut
    }
}
