package io.github.gabrielevanger.petja.core.domain.testing

import io.github.gabrielevanger.petja.core.domain.checkout.CheckoutError
import io.github.gabrielevanger.petja.core.domain.checkout.CheckoutException
import io.github.gabrielevanger.petja.core.domain.checkout.CheckoutRepository
import io.github.gabrielevanger.petja.core.domain.checkout.PaymentChoice
import io.github.gabrielevanger.petja.core.domain.checkout.PlacedOrder
import io.github.gabrielevanger.petja.core.domain.model.Address
import io.github.gabrielevanger.petja.core.domain.model.CartLine
import io.github.gabrielevanger.petja.core.domain.model.Money
import io.github.gabrielevanger.petja.core.domain.model.NewAddress

fun anAddress(id: String = "casa", neighborhood: String = "Centro") = Address(
    id = id,
    label = "Casa",
    street = "Rua Teste",
    number = "10",
    complement = null,
    neighborhood = neighborhood,
    reference = "Perto da praça",
    location = CENTRO,
    isDefault = true,
)

/** Pedido aceito por padrão; `failWith` faz a próxima chamada falhar com o motivo dado. */
class FakeCheckoutRepository(var savedAddresses: List<Address> = emptyList()) : CheckoutRepository {
    var failWith: CheckoutError? = null
    var failAddressesWith: CheckoutError? = null
    val orders = mutableListOf<PlacedOrderCall>()

    data class PlacedOrderCall(
        val storeId: String,
        val addressId: String,
        val lines: List<CartLine>,
        val payment: PaymentChoice,
        val notes: String?,
        val expectedTotal: Money,
    )

    /** Quantas vezes os endereços foram buscados, para conferir que a tela não recarrega à toa. */
    var addressesCalls = 0
        private set

    override suspend fun addresses(): List<Address> {
        addressesCalls++
        failAddressesWith?.let {
            failAddressesWith = null
            throw CheckoutException(it)
        }
        return savedAddresses
    }

    override suspend fun addAddress(address: NewAddress): Address {
        failWith?.let {
            failWith = null
            throw CheckoutException(it)
        }
        val saved = Address(
            id = "novo-${savedAddresses.size}",
            label = null,
            street = address.street,
            number = address.number,
            complement = address.complement,
            neighborhood = address.neighborhood,
            reference = address.reference,
            location = address.location,
            isDefault = true,
        )
        savedAddresses = listOf(saved) + savedAddresses.map { it.copy(isDefault = false) }
        return saved
    }

    override suspend fun placeOrder(
        storeId: String,
        addressId: String,
        lines: List<CartLine>,
        payment: PaymentChoice,
        notes: String?,
        expectedTotal: Money,
    ): PlacedOrder {
        failWith?.let {
            failWith = null
            throw CheckoutException(it)
        }
        orders += PlacedOrderCall(storeId, addressId, lines, payment, notes, expectedTotal)
        return PlacedOrder(
            id = "pedido-${orders.size}",
            number = orders.size.toLong(),
            total = expectedTotal,
        )
    }
}
