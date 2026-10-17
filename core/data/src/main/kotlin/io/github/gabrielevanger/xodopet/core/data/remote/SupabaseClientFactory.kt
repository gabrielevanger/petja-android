package io.github.gabrielevanger.xodopet.core.data.remote

import io.github.gabrielevanger.xodopet.core.data.SupabaseConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.client.engine.HttpClientEngine
import kotlinx.serialization.json.Json

/** Endereço inválido de propósito: sem configuração, as chamadas falham com erro de rede em vez de travar o app. */
private const val UNCONFIGURED_URL = "https://supabase-nao-configurado.invalid"

/**
 * Cria o cliente do Supabase. O motor HTTP é parâmetro para os testes trocarem a rede por respostas fixas.
 * Os módulos do Supabase (Auth, Realtime, Storage) entram aqui conforme as features chegarem.
 */
internal fun createXodoSupabaseClient(config: SupabaseConfig, engine: HttpClientEngine): SupabaseClient =
    createSupabaseClient(
        supabaseUrl = config.url.takeIf { config.isConfigured } ?: UNCONFIGURED_URL,
        supabaseKey = config.anonKey.takeIf { config.isConfigured } ?: "nao-configurado",
    ) {
        httpEngine = engine
        // Campos novos no banco não podem derrubar uma versão antiga do app.
        defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
        install(Postgrest)
    }
