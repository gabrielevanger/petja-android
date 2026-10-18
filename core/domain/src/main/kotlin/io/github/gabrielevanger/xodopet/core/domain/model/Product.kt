package io.github.gabrielevanger.xodopet.core.domain.model

/** Cada tamanho ou apresentação do produto, com preço e estoque próprios (RF-30). */
data class ProductVariant(
    val id: String,
    val label: String,
    val price: Money,
    val stock: Int,
    val isActive: Boolean,
    val sortOrder: Int,
) {
    /** Pausada pela loja ou sem estoque aparece como indisponível (RF-24). */
    val isAvailable: Boolean get() = isActive && stock > 0
}

data class Product(
    val id: String,
    val name: String,
    val description: String?,
    val brand: String?,
    val species: Set<PetSpecies>,
    val imageUrls: List<String>,
    val category: Category,
    val variants: List<ProductVariant>,
) {
    val isAvailable: Boolean get() = variants.any { it.isAvailable }

    /** Menor preço entre as variantes disponíveis, para o "a partir de"; null se nada estiver disponível. */
    val lowestAvailablePrice: Money? get() = variants.filter { it.isAvailable }.minOfOrNull { it.price }
}
