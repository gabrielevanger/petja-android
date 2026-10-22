package io.github.gabrielevanger.petja.core.domain.account

/** O que um link de e-mail aberto no app significou depois de processado. */
enum class AuthLinkResult {
    /** Link de "esqueci a senha": a sessão está aberta e o app pede a senha nova. */
    PasswordRecovery,

    /** Link de confirmação do cadastro: a conta está ativa e conectada. */
    EmailConfirmed,

    /** Link vencido, já usado ou aberto em outro aparelho. */
    Expired,

    /** Não é um link de conta do PetJá; o app ignora. */
    NotAuthLink,
}

interface AuthLinkHandler {
    suspend fun handle(link: String): AuthLinkResult
}
