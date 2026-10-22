package io.github.gabrielevanger.petja.core.domain.checkout

import io.github.gabrielevanger.petja.core.domain.model.Address
import io.github.gabrielevanger.petja.core.domain.model.CartLine
import io.github.gabrielevanger.petja.core.domain.model.Money
import io.github.gabrielevanger.petja.core.domain.model.NewAddress

/** Toda falha chega como [CheckoutException]. */
interface CheckoutRepository {
    suspend fun addresses(): List<Address>

    suspend fun addAddress(address: NewAddress): Address

    /**
     * @param expectedTotal total mostrado ao cliente: se o preço ou a taxa mudaram, o servidor recusa
     *   com [CheckoutError.PriceChanged] em vez de cobrar outro valor (RF-45).
     */
    suspend fun placeOrder(
        storeId: String,
        addressId: String,
        lines: List<CartLine>,
        payment: PaymentChoice,
        notes: String?,
        expectedTotal: Money,
    ): PlacedOrder
}
