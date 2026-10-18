package io.github.gabrielevanger.xodopet.core.data.remote

import io.github.gabrielevanger.xodopet.core.domain.account.Account
import io.github.gabrielevanger.xodopet.core.domain.account.AccountError
import io.github.gabrielevanger.xodopet.core.domain.account.AccountException
import io.github.gabrielevanger.xodopet.core.domain.account.AccountRepository
import io.github.gabrielevanger.xodopet.core.domain.account.AccountState
import io.github.gabrielevanger.xodopet.core.domain.account.SignUpResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.exception.AuthWeakPasswordException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Chave do nome nos metadados do usuário; o gatilho handle_new_user copia para profiles.full_name. */
private const val FULL_NAME_KEY = "full_name"

internal class SupabaseAccountRepository @Inject constructor(private val supabase: SupabaseClient) :
    AccountRepository {

    override val state: Flow<AccountState> = supabase.auth.sessionStatus.map { status ->
        when (status) {
            SessionStatus.Initializing -> AccountState.Loading

            is SessionStatus.NotAuthenticated -> AccountState.SignedOut

            // Falha ao renovar (sem internet, por exemplo) não desconecta: a sessão salva continua valendo.
            is SessionStatus.RefreshFailure, is SessionStatus.Authenticated -> currentAccount()
        }
    }

    override suspend fun signIn(email: String, password: String) = translateErrors {
        supabase.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    override suspend fun signUp(fullName: String, email: String, password: String): SignUpResult = translateErrors {
        supabase.auth.signUpWith(Email, redirectUrl = authLink(AUTH_LINK_CONFIRMED_PATH)) {
            this.email = email.trim()
            this.password = password
            data = buildJsonObject { put(FULL_NAME_KEY, fullName.trim()) }
        }
        // Com a confirmação de e-mail ligada no Supabase, o cadastro não abre sessão até o link ser aberto.
        if (supabase.auth.currentSessionOrNull() != null) SignUpResult.SignedIn else SignUpResult.ConfirmationEmailSent
    }

    override suspend fun sendPasswordReset(email: String) = translateErrors {
        supabase.auth.resetPasswordForEmail(email.trim(), redirectUrl = authLink(AUTH_LINK_RESET_PASSWORD_PATH))
    }

    override suspend fun updatePassword(newPassword: String) = translateErrors {
        supabase.auth.updateUser { password = newPassword }
        Unit
    }

    override suspend fun signOut() = translateErrors { supabase.auth.signOut() }

    private fun currentAccount(): AccountState =
        supabase.auth.currentUserOrNull()?.toAccount()?.let { AccountState.SignedIn(it) } ?: AccountState.SignedOut

    private fun UserInfo.toAccount() = Account(
        id = id,
        email = email.orEmpty(),
        fullName = userMetadata?.get(FULL_NAME_KEY)?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
    )

    private suspend fun <T> translateErrors(block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: AuthWeakPasswordException) {
        throw AccountException(AccountError.WeakPassword, e)
    } catch (e: AuthRestException) {
        throw AccountException(e.errorCode.toAccountError(), e)
    } catch (e: HttpRequestException) {
        throw AccountException(AccountError.Network, e)
    } catch (e: HttpRequestTimeoutException) {
        throw AccountException(AccountError.Network, e)
    } catch (e: Exception) {
        throw AccountException(AccountError.Unknown, e)
    }

    private fun AuthErrorCode?.toAccountError() = when (this) {
        AuthErrorCode.InvalidCredentials -> AccountError.InvalidCredentials
        AuthErrorCode.EmailNotConfirmed -> AccountError.EmailNotConfirmed
        AuthErrorCode.UserAlreadyExists, AuthErrorCode.EmailExists -> AccountError.EmailAlreadyInUse
        AuthErrorCode.WeakPassword -> AccountError.WeakPassword
        AuthErrorCode.SamePassword -> AccountError.SamePassword
        AuthErrorCode.EmailAddressInvalid, AuthErrorCode.ValidationFailed -> AccountError.InvalidEmail
        AuthErrorCode.OverEmailSendRateLimit, AuthErrorCode.OverRequestRateLimit -> AccountError.TooManyEmails
        else -> AccountError.Unknown
    }
}
