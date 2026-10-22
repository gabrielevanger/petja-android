package io.github.gabrielevanger.petja.core.domain.testing

import io.github.gabrielevanger.petja.core.domain.model.ExternalStore
import io.github.gabrielevanger.petja.core.domain.model.GeoPoint
import io.github.gabrielevanger.petja.core.domain.model.StoreSummary
import io.github.gabrielevanger.petja.core.domain.storefront.StorefrontRepository

/** Repositório em memória. `failWith` faz a próxima chamada falhar, para testar erro de rede. */
class FakeStorefrontRepository(
    var stores: List<StoreSummary> = emptyList(),
    var externalStores: List<ExternalStore> = emptyList(),
) : StorefrontRepository {
    var failWith: Throwable? = null
    val storesNearCalls = mutableListOf<GeoPoint>()
    val externalStoresNearCalls = mutableListOf<GeoPoint>()

    override suspend fun storesNear(point: GeoPoint): List<StoreSummary> {
        storesNearCalls += point
        failWith?.let {
            failWith = null
            throw it
        }
        return stores
    }

    override suspend fun externalStoresNear(point: GeoPoint): List<ExternalStore> {
        externalStoresNearCalls += point
        return externalStores
    }
}
