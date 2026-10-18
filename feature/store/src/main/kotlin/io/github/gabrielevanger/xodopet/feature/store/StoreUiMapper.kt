package io.github.gabrielevanger.xodopet.feature.store

import io.github.gabrielevanger.xodopet.core.domain.model.Money
import io.github.gabrielevanger.xodopet.core.domain.model.Product
import io.github.gabrielevanger.xodopet.core.domain.model.StoreDetails
import io.github.gabrielevanger.xodopet.core.domain.store.StoreCatalog
import io.github.gabrielevanger.xodopet.core.ui.formatBrl
import kotlinx.collections.immutable.toImmutableList

internal fun StoreCatalog.toUiState() = StoreUiState.Loaded(
    header = store.toHeader(),
    sections = sections.map { section ->
        CatalogSectionUi(
            categoryId = section.category.id,
            title = section.category.name,
            products = section.products.map { it.toRow() }.toImmutableList(),
        )
    }.toImmutableList(),
)

internal fun StoreDetails.toHeader() = StoreHeaderUi(
    name = name,
    neighborhood = neighborhood,
    description = description,
    isOpen = isOpen,
    estimatedMinutes = estimatedMinutes,
    minOrder = minOrder.takeIf { it > Money.ZERO }?.formatBrl(),
)

internal fun Product.toRow(): ProductRowUi {
    val availablePrices = variants.filter { it.isAvailable }.map { it.price }.distinct()
    return ProductRowUi(
        id = id,
        name = name,
        brand = brand,
        price = lowestAvailablePrice?.formatBrl(),
        fromPrice = availablePrices.size > 1,
        isAvailable = isAvailable,
    )
}

internal fun Product.toDetail() = ProductDetailUi(
    id = id,
    name = name,
    brand = brand,
    description = description,
    species = species.sortedBy { it.ordinal }.toImmutableList(),
    variants = variants.map { VariantUi(it.id, it.label, it.price.formatBrl(), it.isAvailable) }.toImmutableList(),
)
