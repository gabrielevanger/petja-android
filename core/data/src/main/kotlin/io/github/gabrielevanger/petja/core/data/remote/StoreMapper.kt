package io.github.gabrielevanger.petja.core.data.remote

import io.github.gabrielevanger.petja.core.data.remote.dto.CategoryDto
import io.github.gabrielevanger.petja.core.data.remote.dto.ProductDto
import io.github.gabrielevanger.petja.core.data.remote.dto.ProductVariantDto
import io.github.gabrielevanger.petja.core.data.remote.dto.StoreDetailsDto
import io.github.gabrielevanger.petja.core.domain.model.Category
import io.github.gabrielevanger.petja.core.domain.model.Money
import io.github.gabrielevanger.petja.core.domain.model.PetSpecies
import io.github.gabrielevanger.petja.core.domain.model.Product
import io.github.gabrielevanger.petja.core.domain.model.ProductVariant
import io.github.gabrielevanger.petja.core.domain.model.StoreDetails

internal fun StoreDetailsDto.toDomain() = StoreDetails(
    id = id,
    name = name,
    description = description?.takeIf { it.isNotBlank() },
    neighborhood = neighborhood,
    whatsapp = whatsapp?.takeIf { it.isNotBlank() },
    isOpen = isOpen,
    minOrder = Money(minOrderCents),
    estimatedMinutes = estimatedMinutes,
)

internal fun ProductDto.toDomain() = Product(
    id = id,
    name = name,
    description = description?.takeIf { it.isNotBlank() },
    brand = brand?.takeIf { it.isNotBlank() },
    // Espécie nova no banco que o app ainda não conhece vira OTHER em vez de derrubar o catálogo.
    species = species.map { value ->
        PetSpecies.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: PetSpecies.OTHER
    }
        .toSet(),
    imageUrls = imageUrls.filter { it.isNotBlank() },
    category = category.toDomain(),
    variants = variants.map { it.toDomain() },
)

internal fun CategoryDto.toDomain() = Category(id = id, name = name, slug = slug, sortOrder = sortOrder)

internal fun ProductVariantDto.toDomain() = ProductVariant(
    id = id,
    label = label,
    price = Money(priceCents),
    stock = stock,
    isActive = isActive,
    sortOrder = sortOrder,
)
