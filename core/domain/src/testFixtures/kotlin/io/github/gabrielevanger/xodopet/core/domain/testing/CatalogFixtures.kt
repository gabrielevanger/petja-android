package io.github.gabrielevanger.xodopet.core.domain.testing

import io.github.gabrielevanger.xodopet.core.domain.model.Category
import io.github.gabrielevanger.xodopet.core.domain.model.Money
import io.github.gabrielevanger.xodopet.core.domain.model.PetSpecies
import io.github.gabrielevanger.xodopet.core.domain.model.Product
import io.github.gabrielevanger.xodopet.core.domain.model.ProductVariant
import io.github.gabrielevanger.xodopet.core.domain.model.StoreDetails

val RACAO = Category(id = 1, name = "Ração", slug = "racao", sortOrder = 1)
val PETISCOS = Category(id = 2, name = "Petiscos", slug = "petiscos", sortOrder = 2)
val BRINQUEDOS = Category(id = 4, name = "Brinquedos", slug = "brinquedos", sortOrder = 4)

fun aStoreDetails(id: String = "store-1", name: String = "Petshop Lavrado", isOpen: Boolean = true) = StoreDetails(
    id = id,
    name = name,
    description = "Ração, petiscos e acessórios no coração de Boa Vista.",
    neighborhood = "Centro",
    whatsapp = "5595900000001",
    isOpen = isOpen,
    minOrder = Money(3000),
    estimatedMinutes = 40,
)

fun aVariant(
    id: String = "v-1",
    label: String = "1 kg",
    priceCents: Long = 2990,
    stock: Int = 10,
    isActive: Boolean = true,
    sortOrder: Int = 0,
) = ProductVariant(id, label, Money(priceCents), stock, isActive, sortOrder)

fun aProduct(
    id: String = "p-1",
    name: String = "Ração Golden Adultos",
    category: Category = RACAO,
    variants: List<ProductVariant> = listOf(aVariant()),
    brand: String? = "Golden",
    description: String? = "Ração premium para cães adultos.",
) = Product(
    id = id,
    name = name,
    description = description,
    brand = brand,
    species = setOf(PetSpecies.DOG),
    imageUrls = emptyList(),
    category = category,
    variants = variants,
)
