package io.github.gabrielevanger.xodopet.core.domain.orders

import io.github.gabrielevanger.xodopet.core.domain.cart.CartRepository
import io.github.gabrielevanger.xodopet.core.domain.model.Cart
import io.github.gabrielevanger.xodopet.core.domain.model.CartLine
import io.github.gabrielevanger.xodopet.core.domain.model.OrderDetails
import io.github.gabrielevanger.xodopet.core.domain.store.StoreNotFoundException
import io.github.gabrielevanger.xodopet.core.domain.store.StoreRepository

/** Quantos itens do pedido antigo voltaram para o carrinho. */
sealed interface ReorderResult {
    data object AllAdded : ReorderResult

    /** Parte dos itens está indisponível agora ou com menos estoque do que antes. */
    data class SomeChanged(val missing: Int) : ReorderResult

    data object NothingAvailable : ReorderResult
}

/**
 * Pedir de novo (RF-64): troca o carrinho pelos itens do pedido, com o preço e o estoque de agora.
 * O que estiver indisponível fica de fora; o que tiver menos estoque entra com o que houver.
 */
class ReorderToCart(private val stores: StoreRepository, private val carts: CartRepository) {

    suspend operator fun invoke(order: OrderDetails): ReorderResult {
        val store = stores.store(order.storeId) ?: throw StoreNotFoundException(order.storeId)
        val variants = stores.products(order.storeId)
            .flatMap { product -> product.variants.filter { it.isAvailable }.map { product to it } }
            .associateBy { (_, variant) -> variant.id }

        val lines = order.items.mapNotNull { item ->
            val (product, variant) = variants[item.variantId] ?: return@mapNotNull null
            CartLine(
                variantId = variant.id,
                productId = product.id,
                productName = product.name,
                variantLabel = variant.label,
                unitPrice = variant.price,
                quantity = minOf(item.quantity, variant.stock),
                maxQuantity = variant.stock,
            )
        }
        if (lines.isEmpty()) return ReorderResult.NothingAvailable

        carts.update { Cart(store.id, store.name, store.minOrder, lines) }
        val changed = order.items.count { item ->
            lines.none {
                it.variantId == item.variantId &&
                    it.quantity == item.quantity
            }
        }
        return if (changed == 0) ReorderResult.AllAdded else ReorderResult.SomeChanged(changed)
    }
}
