package io.github.gabrielevanger.xodopet.core.data.remote

import io.github.gabrielevanger.xodopet.core.data.remote.dto.ExternalStoreDto
import io.github.gabrielevanger.xodopet.core.data.remote.dto.StoreNearDto
import io.github.gabrielevanger.xodopet.core.domain.model.ExternalStore
import io.github.gabrielevanger.xodopet.core.domain.model.GeoPoint
import io.github.gabrielevanger.xodopet.core.domain.model.StoreSummary
import io.github.gabrielevanger.xodopet.core.domain.storefront.StorefrontRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import javax.inject.Inject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Vitrine pelas funções stores_near e external_stores_near do banco, que funcionam sem login (RF-01). */
internal class SupabaseStorefrontRepository @Inject constructor(private val supabase: SupabaseClient) :
    StorefrontRepository {

    override suspend fun storesNear(point: GeoPoint): List<StoreSummary> = supabase.postgrest
        .rpc("stores_near", point.toParameters())
        .decodeList<StoreNearDto>()
        .map { it.toDomain() }

    override suspend fun externalStoresNear(point: GeoPoint): List<ExternalStore> = supabase.postgrest
        .rpc("external_stores_near", point.toParameters())
        .decodeList<ExternalStoreDto>()
        .map { it.toDomain() }

    private fun GeoPoint.toParameters(): JsonObject = buildJsonObject {
        put("p_lat", latitude)
        put("p_lng", longitude)
    }
}
