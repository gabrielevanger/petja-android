package io.github.gabrielevanger.xodopet.core.data.remote

import io.github.gabrielevanger.xodopet.core.data.SupabaseConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.AuthConfig
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.client.engine.HttpClientEngine
import kotlinx.serialization.json.Json

/** Endereço inválido de propósito: sem configuração, as chamadas falham com erro de rede em vez de travar o app. */
private const val UNCONFIGURED_URL = "https://supabase-nao-configurado.invalid"

/** Links dos e-mails de conta voltam para o app em xodopet://auth/... (declarado no AndroidManifest do app). */
internal const val AUTH_LINK_SCHEME = "xodopet"
internal const val AUTH_LINK_HOST = "auth"
internal const val AUTH_LINK_CONFIRMED_PATH = "/confirmed"
internal const val AUTH_LINK_RESET_PASSWORD_PATH = "/reset-password"

internal fun authLink(path: String) = "$AUTH_LINK_SCHEME://$AUTH_LINK_HOST$path"

/**
 * Cria o cliente do Supabase. O motor HTTP e os ajustes de Auth são parâmetros para os testes trocarem
 * a rede por respostas fixas e a sessão salva no aparelho por uma em memória.
 */
internal fun createXodoSupabaseClient(
    config: SupabaseConfig,
    engine: HttpClientEngine,
    authOverrides: AuthConfig.() -> Unit = {},
): SupabaseClient = createSupabaseClient(
    supabaseUrl = config.url.takeIf { config.isConfigured } ?: UNCONFIGURED_URL,
    supabaseKey = config.anonKey.takeIf { config.isConfigured } ?: "nao-configurado",
) {
    httpEngine = engine
    // Campos novos no banco não podem derrubar uma versão antiga do app.
    defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
    install(Postgrest)
    install(Auth) {
        scheme = AUTH_LINK_SCHEME
        host = AUTH_LINK_HOST
        // PKCE: o link do e-mail traz um código que só este aparelho consegue trocar por uma sessão.
        flowType = FlowType.PKCE
        authOverrides()
    }
    // Status do pedido ao vivo (RF-60). Só conecta quando alguém escuta um canal.
    install(Realtime)
}
