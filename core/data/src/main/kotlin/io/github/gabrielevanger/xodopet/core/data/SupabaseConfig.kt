package io.github.gabrielevanger.xodopet.core.data

/**
 * Projeto Supabase usado pelo app. Vem do local.properties pelo módulo app (ver README).
 * A chave é a pública (anon): quem protege os dados é a RLS do banco, não o segredo da chave (RNF-05).
 */
data class SupabaseConfig(val url: String, val anonKey: String) {
    val isConfigured: Boolean get() = url.isNotBlank() && anonKey.isNotBlank()
}
