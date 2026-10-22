package io.github.gabrielevanger.petja.core.domain.store

import io.github.gabrielevanger.petja.core.domain.model.Category
import io.github.gabrielevanger.petja.core.domain.model.Product
import io.github.gabrielevanger.petja.core.domain.model.StoreDetails

data class CatalogSection(val category: Category, val products: List<Product>)

/** Página da loja: dados da loja e produtos agrupados por categoria (RF-22). */
data class StoreCatalog(val store: StoreDetails, val sections: List<CatalogSection>)

/**
 * Agrupa os produtos por categoria, na ordem das categorias. Dentro de cada uma, os disponíveis
 * vêm antes e depois por nome; as variantes seguem a ordem que a loja definiu.
 */
fun groupByCategory(products: List<Product>): List<CatalogSection> = products
    .map { product -> product.copy(variants = product.variants.sortedBy { it.sortOrder }) }
    .groupBy { it.category }
    .entries
    .sortedWith(compareBy({ it.key.sortOrder }, { it.key.name }))
    .map { (category, items) ->
        CatalogSection(
            category = category,
            products = items.sortedWith(compareByDescending<Product> { it.isAvailable }.thenBy { it.name }),
        )
    }
