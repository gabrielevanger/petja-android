package io.github.gabrielevanger.xodopet.core.domain.model

/**
 * Um item no carrinho. Nome, opção e preço são cópias do momento em que foi adicionado; o servidor
 * confere tudo de novo ao fazer o pedido (RF-45).
 *
 * @property maxQuantity estoque da opção quando foi adicionada: o carrinho não deixa passar disso.
 */
data class CartLine(
    val variantId: String,
    val productId: String,
    val productName: String,
    val variantLabel: String,
    val unitPrice: Money,
    val quantity: Int,
    val maxQuantity: Int,
) {
    val total: Money get() = Money(unitPrice.cents * quantity)
}

/** Carrinho de uma única loja (RF-41, RN-01). */
data class Cart(val storeId: String, val storeName: String, val minOrder: Money, val lines: List<CartLine>) {
    val subtotal: Money get() = Money(lines.sumOf { it.total.cents })

    val itemCount: Int get() = lines.sumOf { it.quantity }

    /** Quanto falta para o pedido mínimo da loja; zero quando já atingiu (RF-44, RN-09). */
    val missingForMinOrder: Money get() = Money(maxOf(0, minOrder.cents - subtotal.cents))

    val meetsMinOrder: Boolean get() = missingForMinOrder == Money.ZERO
}
