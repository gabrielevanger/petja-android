package io.github.gabrielevanger.petja.core.data.remote

import io.github.gabrielevanger.petja.core.domain.account.Account
import io.github.gabrielevanger.petja.core.domain.account.AccountError
import io.github.gabrielevanger.petja.core.domain.account.AccountException
import io.github.gabrielevanger.petja.core.domain.account.AccountState
import io.github.gabrielevanger.petja.core.domain.account.AuthLinkResult
import io.github.gabrielevanger.petja.core.domain.account.SignUpResult
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SupabaseAccountRepositoryTest {
    private val requests = mutableListOf<Pair<HttpRequestData, String>>()
    private val warnings = mutableListOf<String>()

    /** Respostas do Supabase Auth por caminho; o que não estiver no mapa responde 404. */
    private var routes: Map<String, MockRequestHandleScope.() -> HttpResponseData> = emptyMap()

    private val client = testSupabaseClient(
        MockEngine { request ->
            requests += request to String(request.body.toByteArray())
            routes[request.url.encodedPath]?.invoke(this) ?: respond("", HttpStatusCode.NotFound)
        },
    )
    private val repository = SupabaseAccountRepository(client)
    private val links = SupabaseAuthLinkHandler(client) { message, _ -> warnings += message }

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun MockRequestHandleScope.session() = json(
        """
        {"access_token":"jwt","refresh_token":"r","token_type":"bearer","expires_in":3600,
         "user":{"id":"u1","aud":"authenticated","email":"ana@teste.local","user_metadata":{"full_name":"Ana Souza"}}}
        """.trimIndent(),
    )

    private fun MockRequestHandleScope.authError(code: String, status: HttpStatusCode = HttpStatusCode.BadRequest) =
        json("""{"code":${status.value},"error_code":"$code","msg":"erro"}""", status)

    private suspend fun assertFails(error: AccountError, block: suspend () -> Unit) {
        val exception = assertThrows<AccountException> { block() }
        assertEquals(error, exception.error)
    }

    @Test
    fun `starts signed out and signs in with email and password`() = runTest {
        routes = mapOf("/auth/v1/token" to { session() })
        assertEquals(AccountState.SignedOut, repository.state.first { it != AccountState.Loading })

        repository.signIn(" ana@teste.local ", "segredo123")

        val (request, body) = requests.single()
        assertEquals("password", request.url.parameters["grant_type"])
        assertTrue(body.contains("\"email\":\"ana@teste.local\""), body)
        assertEquals(
            AccountState.SignedIn(Account("u1", "ana@teste.local", "Ana Souza")),
            repository.state.first { it is AccountState.SignedIn },
        )
    }

    @Test
    fun `server errors become reasons the screen can explain`() = runTest {
        routes = mapOf("/auth/v1/token" to { authError("invalid_credentials") })
        assertFails(AccountError.InvalidCredentials) { repository.signIn("ana@teste.local", "errada123") }

        routes = mapOf("/auth/v1/token" to { authError("email_not_confirmed") })
        assertFails(AccountError.EmailNotConfirmed) { repository.signIn("ana@teste.local", "segredo123") }

        routes = mapOf("/auth/v1/signup" to { authError("user_already_exists", HttpStatusCode.UnprocessableEntity) })
        assertFails(AccountError.EmailAlreadyInUse) { repository.signUp("Ana", "ana@teste.local", "segredo123") }

        routes =
            mapOf("/auth/v1/recover" to { authError("over_email_send_rate_limit", HttpStatusCode.TooManyRequests) })
        assertFails(AccountError.TooManyEmails) { repository.sendPasswordReset("ana@teste.local") }

        routes = mapOf("/auth/v1/token" to { authError("algo_novo") })
        assertFails(AccountError.Unknown) { repository.signIn("ana@teste.local", "segredo123") }
    }

    @Test
    fun `network failure`() = runTest {
        routes = mapOf("/auth/v1/token" to { throw IOException("sem rede") })

        assertFails(AccountError.Network) { repository.signIn("ana@teste.local", "segredo123") }
    }

    @Test
    fun `sign up sends the name and waits for the confirmation email`() = runTest {
        routes = mapOf(
            "/auth/v1/signup" to { json("""{"id":"u2","aud":"authenticated","email":"bia@teste.local"}""") },
        )

        val result = repository.signUp(" Bia Lima ", "bia@teste.local", "segredo123")

        assertEquals(SignUpResult.ConfirmationEmailSent, result)
        val (request, body) = requests.single()
        assertTrue(body.contains("\"full_name\":\"Bia Lima\""), body)
        assertEquals("petja://auth/confirmed", request.url.parameters["redirect_to"])
    }

    @Test
    fun `sign up without email confirmation signs in right away`() = runTest {
        routes = mapOf("/auth/v1/signup" to { session() })

        assertEquals(SignUpResult.SignedIn, repository.signUp("Ana", "ana@teste.local", "segredo123"))
    }

    @Test
    fun `password reset link opens the session and the new password is saved`() = runTest {
        routes = mapOf(
            "/auth/v1/recover" to { json("{}") },
            "/auth/v1/token" to { session() },
            "/auth/v1/user" to { json("""{"id":"u1","aud":"authenticated","email":"ana@teste.local"}""") },
        )

        repository.sendPasswordReset("ana@teste.local")
        val recover = requests.last().first
        assertEquals("petja://auth/reset-password", recover.url.parameters["redirect_to"])

        assertEquals(AuthLinkResult.PasswordRecovery, links.handle("petja://auth/reset-password?code=abc123"))
        assertEquals("pkce", requests.last().first.url.parameters["grant_type"])

        repository.updatePassword("novasenha123")
        assertEquals(HttpMethod.Put, requests.last().first.method)
        assertTrue(requests.last().second.contains("novasenha123"))
    }

    @Test
    fun `confirmation link signs in`() = runTest {
        routes = mapOf(
            "/auth/v1/signup" to { json("""{"id":"u1","aud":"authenticated","email":"ana@teste.local"}""") },
            "/auth/v1/token" to { session() },
        )
        repository.signUp("Ana", "ana@teste.local", "segredo123")

        assertEquals(AuthLinkResult.EmailConfirmed, links.handle("petja://auth/confirmed?code=xyz"))
        assertTrue(repository.state.first { it != AccountState.Loading } is AccountState.SignedIn)
    }

    @Test
    fun `expired or foreign links`() = runTest {
        routes = mapOf("/auth/v1/token" to { authError("flow_state_expired") })

        assertEquals(AuthLinkResult.NotAuthLink, links.handle("https://petja.com.br"))
        assertEquals(AuthLinkResult.NotAuthLink, links.handle("não é um link"))
        assertEquals(
            AuthLinkResult.Expired,
            links.handle("petja://auth/reset-password?error=access_denied&error_description=Email+link+is+invalid"),
        )
        assertEquals(AuthLinkResult.Expired, links.handle("petja://auth/confirmed?code=velho"))
        assertEquals(2, warnings.size)
    }

    @Test
    fun `sign out`() = runTest {
        routes =
            mapOf("/auth/v1/token" to { session() }, "/auth/v1/logout" to { respond("", HttpStatusCode.NoContent) })
        repository.signIn("ana@teste.local", "segredo123")

        repository.signOut()

        assertEquals(AccountState.SignedOut, repository.state.first())
    }
}
