package io.github.gabrielevanger.petja.core.data.remote

import io.github.gabrielevanger.petja.core.data.SupabaseConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.MemorySessionManager
import io.ktor.client.engine.HttpClientEngine

/** Cliente com a rede trocada pelo motor de teste e a sessão em memória, sem o armazenamento do Android. */
internal fun testSupabaseClient(engine: HttpClientEngine): SupabaseClient = createPetjaSupabaseClient(
    SupabaseConfig("https://teste.supabase.co", "chave-anon"),
    engine,
) {
    sessionManager = MemorySessionManager()
    codeVerifierCache = MemoryCodeVerifierCache()
    autoLoadFromStorage = false
    alwaysAutoRefresh = false
    enableLifecycleCallbacks = false
}
