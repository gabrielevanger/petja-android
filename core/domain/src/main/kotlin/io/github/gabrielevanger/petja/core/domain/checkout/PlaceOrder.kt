package io.github.gabrielevanger.petja.core.domain.checkout

import io.github.gabrielevanger.petja.core.domain.cart.CartActions
import io.github.gabrielevanger.petja.core.domain.model.Address
import io.github.gabrielevanger.petja.core.domain.model.Cart
import io.github.gabrielevanger.petja.core.domain.model.Money

/**
 * Faz o pedido do carrinho e, se o servidor aceitar, esvazia o carrinho. O que o servidor confere
 * (loja aberta, raio, estoque, mínimo, preço) não é repetido aqui; só o troco, que o servidor não sabe
 * comparar com o total.
 */
class PlaceOrder(private val repository: CheckoutRepository, private val cartActions: CartActions) {

    suspend operator fun invoke(
        cart: Cart,
        address: Address,
        payment: PaymentChoice,
        notes: String?,
        deliveryFee: Money,
    ): PlacedOrder {
        if (cart.lines.isEmpty()) throw CheckoutException(CheckoutError.EmptyCart)
        val total = cart.subtotal + deliveryFee
        if (payment is PaymentChoice.Cash && payment.changeFor != null && payment.changeFor <= total) {
            throw CheckoutException(CheckoutError.ChangeTooLow)
        }
        val order = repository.placeOrder(
            storeId = cart.storeId,
            addressId = address.id,
            lines = cart.lines,
            payment = payment,
            notes = notes?.trim()?.takeIf { it.isNotEmpty() },
            expectedTotal = total,
        )
        cartActions.clear()
        return order
    }
}
