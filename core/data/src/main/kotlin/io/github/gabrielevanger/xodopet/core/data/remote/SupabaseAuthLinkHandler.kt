package io.github.gabrielevanger.xodopet.core.data.remote

import io.github.gabrielevanger.xodopet.core.domain.DiagnosticLog
import io.github.gabrielevanger.xodopet.core.domain.account.AuthLinkHandler
import io.github.gabrielevanger.xodopet.core.domain.account.AuthLinkResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import java.net.URI
import java.net.URLDecoder
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * Abre a sessão a partir do link do e-mail (xodopet://auth/...?code=...). O caminho diz o que o link era:
 * confirmação de cadastro ou redefinição de senha.
 */
private const val UTF_8 = "UTF-8"

internal class SupabaseAuthLinkHandler @Inject constructor(
    private val supabase: SupabaseClient,
    private val log: DiagnosticLog,
) : AuthLinkHandler {

    override suspend fun handle(link: String): AuthLinkResult {
        val uri = runCatching { URI(link) }.getOrNull() ?: return AuthLinkResult.NotAuthLink
        if (uri.scheme != AUTH_LINK_SCHEME || uri.host != AUTH_LINK_HOST) return AuthLinkResult.NotAuthLink
        val query = uri.rawQuery.orEmpty().split('&').filter { it.contains('=') }.associate {
            val (key, value) = it.split('=', limit = 2)
            // A versão com Charset só existe a partir do Android 13; a com o nome funciona desde o 8 (minSdk).
            key to URLDecoder.decode(value, UTF_8)
        }
        val code = query["code"]
        if (query.containsKey("error") || code == null) {
            log.warn("Link de conta sem código: ${query["error_description"] ?: query["error"]}", null)
            return AuthLinkResult.Expired
        }
        return try {
            supabase.auth.exchangeCodeForSession(code)
            if (uri.path ==
                AUTH_LINK_RESET_PASSWORD_PATH
            ) {
                AuthLinkResult.PasswordRecovery
            } else {
                AuthLinkResult.EmailConfirmed
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Link vencido, já usado ou aberto em outro aparelho (sem o código PKCE guardado aqui).
            log.warn("Falha ao abrir o link de conta", e)
            AuthLinkResult.Expired
        }
    }
}
