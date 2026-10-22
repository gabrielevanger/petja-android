package io.github.gabrielevanger.petja.core.domain.store

import io.github.gabrielevanger.petja.core.domain.model.Product
import io.github.gabrielevanger.petja.core.domain.model.StoreDetails

interface StoreRepository {
    /** Loja ativa (ou da equipe de quem pede); null se não existir ou não estiver visível. */
    suspend fun store(storeId: String): StoreDetails?

    /** Produtos ativos da loja, com categoria e todas as variantes (inclusive as indisponíveis). */
    suspend fun products(storeId: String): List<Product>
}
