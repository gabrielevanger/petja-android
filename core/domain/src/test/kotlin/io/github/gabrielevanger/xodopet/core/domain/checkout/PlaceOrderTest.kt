package io.github.gabrielevanger.xodopet.core.domain.checkout

import io.github.gabrielevanger.xodopet.core.domain.account.FieldError
import io.github.gabrielevanger.xodopet.core.domain.cart.CartActions
import io.github.gabrielevanger.xodopet.core.domain.model.Cart
import io.github.gabrielevanger.xodopet.core.domain.model.CartLine
import io.github.gabrielevanger.xodopet.core.domain.model.Money
import io.github.gabrielevanger.xodopet.core.domain.testing.FakeCartRepository
import io.github.gabrielevanger.xodopet.core.domain.testing.FakeCheckoutRepository
import io.github.gabrielevanger.xodopet.core.domain.testing.anAddress
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PlaceOrderTest {
    private val line = CartLine("1kg", "golden", "Ração Golden", "1 kg", Money(3090), quantity = 2, maxQuantity = 5)
    private val cart = Cart("store-1", "Petshop Lavrado", Money(3000), listOf(line))
    private val carts = FakeCartRepository(cart)
    private val repository = FakeCheckoutRepository()
    private val placeOrder = PlaceOrder(repository, CartActions(carts))

    @Test
    fun `places the order with the expected total and empties the cart`() = runTest {
        val order = placeOrder(cart, anAddress(), PaymentChoice.PixOnDelivery, "  Tocar a campainha ", Money(500))

        val call = repository.orders.single()
        assertEquals("store-1", call.storeId)
        assertEquals("casa", call.addressId)
        assertEquals(Money(2 * 3090L + 500), call.expectedTotal)
        assertEquals("Tocar a campainha", call.notes)
        assertEquals(1L, order.number)
        assertNull(carts.cart.value)
    }

    @Test
    fun `blank notes are not sent`() = runTest {
        placeOrder(cart, anAddress(), PaymentChoice.CardOnDelivery, "   ", Money(0))

        assertNull(repository.orders.single().notes)
    }

    @Test
    fun `change must be more than the total`() = runTest {
        val exception = assertThrows<CheckoutException> {
            placeOrder(cart, anAddress(), PaymentChoice.Cash(changeFor = Money(6680)), null, Money(500))
        }

        assertEquals(CheckoutError.ChangeTooLow, exception.error)
        assertEquals(0, repository.orders.size)
        assertNotNull(carts.cart.value, "o carrinho continua")
    }

    @Test
    fun `cash with change or exact amount`() = runTest {
        placeOrder(cart, anAddress(), PaymentChoice.Cash(changeFor = Money(10000)), null, Money(500))

        assertEquals(PaymentChoice.Cash(Money(10000)), repository.orders.single().payment)
    }

    @Test
    fun `server refusal keeps the cart`() = runTest {
        repository.failWith = CheckoutError.StoreClosed

        val exception =
            assertThrows<CheckoutException> {
                placeOrder(cart, anAddress(), PaymentChoice.Cash(null), null, Money(500))
            }

        assertEquals(CheckoutError.StoreClosed, exception.error)
        assertEquals("StoreClosed", exception.message)
        assertNotNull(carts.cart.value)
    }

    @Test
    fun `empty cart is refused before calling the server`() = runTest {
        val exception = assertThrows<CheckoutException> {
            placeOrder(cart.copy(lines = emptyList()), anAddress(), PaymentChoice.PixOnDelivery, null, Money(0))
        }

        assertEquals(CheckoutError.EmptyCart, exception.error)
    }

    @Test
    fun `address fields`() {
        assertEquals(FieldError.Required, AddressValidation.required("  "))
        assertNull(AddressValidation.required("Rua A"))
        assertEquals(11, CheckoutError.entries.size)
    }
}
