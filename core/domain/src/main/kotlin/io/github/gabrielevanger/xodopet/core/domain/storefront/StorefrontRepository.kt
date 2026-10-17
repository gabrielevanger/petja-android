package io.github.gabrielevanger.xodopet.core.domain.storefront

import io.github.gabrielevanger.xodopet.core.domain.model.ExternalStore
import io.github.gabrielevanger.xodopet.core.domain.model.GeoPoint
import io.github.gabrielevanger.xodopet.core.domain.model.StoreSummary

interface StorefrontRepository {
    /** Lojas parceiras ativas cujo raio de entrega cobre o ponto (RN-02). */
    suspend fun storesNear(point: GeoPoint): List<StoreSummary>

    /** Petshops não parceiros mais próximos do ponto (RF-15). */
    suspend fun externalStoresNear(point: GeoPoint): List<ExternalStore>
}
