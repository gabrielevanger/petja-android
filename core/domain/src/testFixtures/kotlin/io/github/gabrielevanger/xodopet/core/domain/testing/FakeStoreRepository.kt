package io.github.gabrielevanger.xodopet.core.domain.testing

import io.github.gabrielevanger.xodopet.core.domain.model.Product
import io.github.gabrielevanger.xodopet.core.domain.model.StoreDetails
import io.github.gabrielevanger.xodopet.core.domain.store.StoreRepository

/** Repositório em memória. `failWith` faz a próxima busca da loja falhar, para testar erro de rede. */
class FakeStoreRepository(
    var stores: Map<String, StoreDetails> = mapOf("store-1" to aStoreDetails()),
    var products: List<Product> = listOf(aProduct()),
) : StoreRepository {
    var failWith: Throwable? = null

    /** Quantas vezes a loja foi buscada, para conferir que a tela não recarrega à toa. */
    var catalogCalls = 0
        private set

    override suspend fun store(storeId: String): StoreDetails? {
        catalogCalls++
        failWith?.let {
            failWith = null
            throw it
        }
        return stores[storeId]
    }

    override suspend fun products(storeId: String): List<Product> = products
}
