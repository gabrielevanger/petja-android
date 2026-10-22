package io.github.gabrielevanger.petja.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Linha da tabela stores, com as colunas de [STORE_COLUMNS]. */
@Serializable
internal data class StoreDetailsDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val neighborhood: String,
    val whatsapp: String? = null,
    @SerialName("is_open") val isOpen: Boolean,
    @SerialName("min_order_cents") val minOrderCents: Long,
    @SerialName("estimated_minutes") val estimatedMinutes: Int,
)

/** Produto com a categoria e as variantes embutidas, como pede [PRODUCT_COLUMNS]. */
@Serializable
internal data class ProductDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val brand: String? = null,
    val species: List<String> = emptyList(),
    @SerialName("image_urls") val imageUrls: List<String> = emptyList(),
    val category: CategoryDto,
    val variants: List<ProductVariantDto> = emptyList(),
)

@Serializable
internal data class CategoryDto(
    val id: Int,
    val name: String,
    val slug: String,
    @SerialName("sort_order") val sortOrder: Int,
)

@Serializable
internal data class ProductVariantDto(
    val id: String,
    val label: String,
    @SerialName("price_cents") val priceCents: Long,
    val stock: Int,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("sort_order") val sortOrder: Int,
)

internal const val STORE_COLUMNS =
    "id,name,description,neighborhood,whatsapp,is_open,min_order_cents,estimated_minutes"

// Uma chamada só: o PostgREST embute a categoria e as variantes pelas chaves estrangeiras.
internal const val PRODUCT_COLUMNS =
    "id,name,description,brand,species,image_urls," +
        "category:categories(id,name,slug,sort_order)," +
        "variants:product_variants(id,label,price_cents,stock,is_active,sort_order)"
