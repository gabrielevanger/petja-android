package io.github.gabrielevanger.petja.core.domain.cart

import io.github.gabrielevanger.petja.core.domain.model.Cart
import io.github.gabrielevanger.petja.core.domain.model.CartLine
import io.github.gabrielevanger.petja.core.domain.model.Product
import io.github.gabrielevanger.petja.core.domain.model.ProductVariant
import io.github.gabrielevanger.petja.core.domain.model.StoreDetails

sealed interface AddToCartResult {
    data object Added : AddToCartResult

    /** O carrinho tem itens de outra loja: o app pergunta antes de esvaziar (RF-41). */
    data class OtherStoreInCart(val storeName: String) : AddToCartResult

    /** A opção está pausada ou sem estoque (RN-10). */
    data object Unavailable : AddToCartResult

    /** Já tem no carrinho tudo o que a loja tem em estoque dessa opção. */
    data object LimitReached : AddToCartResult
}

/** Regras do carrinho: uma loja por vez, nada indisponível e nada além do estoque (RF-40, RF-41). */
class CartActions(private val repository: CartRepository) {

    suspend fun add(
        store: StoreDetails,
        product: Product,
        variant: ProductVariant,
        replaceOtherStore: Boolean = false,
    ): AddToCartResult {
        if (!variant.isAvailable) return AddToCartResult.Unavailable
        var result: AddToCartResult = AddToCartResult.Added
        repository.update { current ->
            when {
                current != null && current.storeId != store.id && !replaceOtherStore -> {
                    result = AddToCartResult.OtherStoreInCart(current.storeName)
                    current
                }

                else -> {
                    val cart = current?.takeIf { it.storeId == store.id }
                        ?: Cart(store.id, store.name, store.minOrder, emptyList())
                    val existing = cart.lines.firstOrNull { it.variantId == variant.id }
                    if (existing != null && existing.quantity >= variant.stock) {
                        result = AddToCartResult.LimitReached
                        cart
                    } else {
                        cart.withLine(variant.id) { line ->
                            line?.copy(quantity = line.quantity + 1, maxQuantity = variant.stock)
                                ?: CartLine(
                                    variantId = variant.id,
                                    productId = product.id,
                                    productName = product.name,
                                    variantLabel = variant.label,
                                    unitPrice = variant.price,
                                    quantity = 1,
                                    maxQuantity = variant.stock,
                                )
                        }.copy(storeName = store.name, minOrder = store.minOrder)
                    }
                }
            }
        }
        return result
    }

    /** Muda a quantidade, limitada ao estoque; zero ou menos remove o item, e sem itens o carrinho some. */
    suspend fun setQuantity(variantId: String, quantity: Int) {
        repository.update { cart ->
            cart?.withLine(variantId) { line ->
                line?.copy(quantity = quantity.coerceAtMost(line.maxQuantity))?.takeIf { it.quantity > 0 }
            }?.takeIf { it.lines.isNotEmpty() }
        }
    }

    suspend fun remove(variantId: String) = setQuantity(variantId, 0)

    suspend fun clear() = repository.update { null }

    /** Troca (ou cria, ou remove com null) a linha da opção, mantendo a ordem em que os itens entraram. */
    private fun Cart.withLine(variantId: String, change: (CartLine?) -> CartLine?): Cart {
        val index = lines.indexOfFirst { it.variantId == variantId }
        val updated = change(lines.getOrNull(index))
        val newLines = when {
            index < 0 -> lines + listOfNotNull(updated)
            updated == null -> lines.filterIndexed { i, _ -> i != index }
            else -> lines.mapIndexed { i, line -> if (i == index) updated else line }
        }
        return copy(lines = newLines)
    }
}
