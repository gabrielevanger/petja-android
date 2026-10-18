package io.github.gabrielevanger.xodopet.core.data.remote

import io.github.gabrielevanger.xodopet.core.data.remote.dto.PRODUCT_COLUMNS
import io.github.gabrielevanger.xodopet.core.data.remote.dto.ProductDto
import io.github.gabrielevanger.xodopet.core.data.remote.dto.STORE_COLUMNS
import io.github.gabrielevanger.xodopet.core.data.remote.dto.StoreDetailsDto
import io.github.gabrielevanger.xodopet.core.domain.model.Product
import io.github.gabrielevanger.xodopet.core.domain.model.StoreDetails
import io.github.gabrielevanger.xodopet.core.domain.store.StoreRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject

/**
 * Leitura direta das tabelas: a RLS já mostra só lojas ativas e produtos ativos para o público,
 * e o catálogo inteiro (inclusive pausados) para a equipe da loja.
 */
internal class SupabaseStoreRepository @Inject constructor(private val supabase: SupabaseClient) : StoreRepository {

    override suspend fun store(storeId: String): StoreDetails? = supabase.from("stores")
        .select(Columns.raw(STORE_COLUMNS)) { filter { eq("id", storeId) } }
        .decodeList<StoreDetailsDto>()
        .firstOrNull()
        ?.toDomain()

    override suspend fun products(storeId: String): List<Product> = supabase.from("products")
        .select(Columns.raw(PRODUCT_COLUMNS)) {
            filter {
                eq("store_id", storeId)
                eq("is_active", true)
            }
        }
        .decodeList<ProductDto>()
        .map { it.toDomain() }
}
