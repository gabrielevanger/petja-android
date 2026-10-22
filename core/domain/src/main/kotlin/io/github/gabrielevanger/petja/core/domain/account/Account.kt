package io.github.gabrielevanger.petja.core.domain.account

/** Quem está usando o app. O nome vem do cadastro; contas antigas ou de outros provedores podem não ter. */
data class Account(val id: String, val email: String, val fullName: String?)

sealed interface AccountState {
    /** O app ainda está lendo a sessão salva no aparelho. */
    data object Loading : AccountState

    data object SignedOut : AccountState

    data class SignedIn(val account: Account) : AccountState
}

/** Resultado do cadastro: com confirmação de e-mail ligada no Supabase, a conta só vale depois do link. */
enum class SignUpResult { SignedIn, ConfirmationEmailSent }

/** Motivos de falha que a tela sabe explicar. */
enum class AccountError {
    InvalidCredentials,
    EmailNotConfirmed,
    EmailAlreadyInUse,
    WeakPassword,
    SamePassword,
    InvalidEmail,
    TooManyEmails,
    Network,
    Unknown,
}

class AccountException(val error: AccountError, cause: Throwable? = null) : Exception(error.name, cause)
