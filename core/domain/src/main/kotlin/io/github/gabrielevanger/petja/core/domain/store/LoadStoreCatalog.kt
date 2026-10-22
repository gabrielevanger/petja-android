package io.github.gabrielevanger.petja.core.domain.store

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Loja não encontrada ou não visível para quem pede (desativada, removida ou link antigo). */
class StoreNotFoundException(storeId: String) : Exception("Loja $storeId não encontrada")

class LoadStoreCatalog(private val repository: StoreRepository) {
    /** Loja e produtos são buscados ao mesmo tempo: a página abre no tempo da chamada mais lenta, não da soma. */
    suspend operator fun invoke(storeId: String): StoreCatalog = coroutineScope {
        val products = async { repository.products(storeId) }
        val store = repository.store(storeId) ?: throw StoreNotFoundException(storeId)
        StoreCatalog(store, groupByCategory(products.await()))
    }
}
