package io.github.gabrielevanger.petja.core.domain.account

import kotlinx.coroutines.flow.Flow

/** Conta por e-mail e senha (RF-02, RF-04). Toda falha chega como [AccountException]. */
interface AccountRepository {
    val state: Flow<AccountState>

    suspend fun signIn(email: String, password: String)

    suspend fun signUp(fullName: String, email: String, password: String): SignUpResult

    /** Manda o link de redefinição. Não diz se o e-mail tem conta, para não revelar quem é cliente. */
    suspend fun sendPasswordReset(email: String)

    /** Troca a senha da sessão atual, que vem do link de redefinição. */
    suspend fun updatePassword(newPassword: String)

    suspend fun signOut()
}
