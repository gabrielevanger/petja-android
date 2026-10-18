package io.github.gabrielevanger.xodopet.core.domain.cart

import io.github.gabrielevanger.xodopet.core.domain.model.Money
import io.github.gabrielevanger.xodopet.core.domain.testing.FakeCartRepository
import io.github.gabrielevanger.xodopet.core.domain.testing.aProduct
import io.github.gabrielevanger.xodopet.core.domain.testing.aStoreDetails
import io.github.gabrielevanger.xodopet.core.domain.testing.aVariant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CartActionsTest {
    private val repository = FakeCartRepository()
    private val actions = CartActions(repository)

    private val lavrado = aStoreDetails(id = "lavrado", name = "Petshop Lavrado")
    private val casaDoBicho = aStoreDetails(id = "casa", name = "Casa do Bicho")
    private val golden = aProduct(id = "golden", name = "Ração Golden")
    private val umKg = aVariant(id = "1kg", label = "1 kg", priceCents = 3090, stock = 3)
    private val quinzeKg = aVariant(id = "15kg", label = "15 kg", priceCents = 22890, stock = 1)

    private val cart get() = repository.cart.value

    @Test
    fun `first item creates the cart of that store`() = runTest {
        assertEquals(AddToCartResult.Added, actions.add(lavrado, golden, umKg))

        assertEquals("lavrado", cart?.storeId)
        assertEquals(1, cart?.itemCount)
        assertEquals(Money(3090), cart?.subtotal)
    }

    @Test
    fun `adding the same option again increases the quantity`() = runTest {
        actions.add(lavrado, golden, umKg)
        actions.add(lavrado, golden, umKg)
        actions.add(lavrado, golden, quinzeKg)

        assertEquals(listOf(2, 1), cart?.lines?.map { it.quantity })
        assertEquals(Money(2 * 3090L + 22890), cart?.subtotal)
    }

    @Test
    fun `cannot go past the stock`() = runTest {
        actions.add(lavrado, golden, quinzeKg)

        assertEquals(AddToCartResult.LimitReached, actions.add(lavrado, golden, quinzeKg))
        assertEquals(1, cart?.itemCount)
    }

    @Test
    fun `unavailable option is refused`() = runTest {
        assertEquals(AddToCartResult.Unavailable, actions.add(lavrado, golden, aVariant(stock = 0)))
        assertNull(cart)
    }

    @Test
    fun `another store asks before replacing`() = runTest {
        actions.add(lavrado, golden, umKg)

        val result = actions.add(casaDoBicho, golden, umKg)

        assertEquals(AddToCartResult.OtherStoreInCart("Petshop Lavrado"), result)
        assertEquals("lavrado", cart?.storeId, "o carrinho antigo continua")
    }

    @Test
    fun `replacing starts a new cart for the other store`() = runTest {
        actions.add(lavrado, golden, umKg)
        actions.add(lavrado, golden, umKg)

        assertEquals(AddToCartResult.Added, actions.add(casaDoBicho, golden, umKg, replaceOtherStore = true))

        assertEquals("casa", cart?.storeId)
        assertEquals(1, cart?.itemCount)
    }

    @Test
    fun `quantity is limited to the stock and zero removes`() = runTest {
        actions.add(lavrado, golden, umKg)
        actions.add(lavrado, golden, quinzeKg)

        actions.setQuantity("1kg", 10)
        assertEquals(3, cart?.lines?.first()?.quantity)

        actions.setQuantity("1kg", 0)
        assertEquals(listOf("15kg"), cart?.lines?.map { it.variantId })

        actions.remove("15kg")
        assertNull(cart, "sem itens, o carrinho some")
    }

    @Test
    fun `minimum order`() = runTest {
        actions.add(lavrado, golden, umKg)

        assertEquals(Money(0), cart?.missingForMinOrder)
        assertTrue(cart!!.meetsMinOrder)

        val cheap = aVariant(id = "sache", priceCents = 390)
        actions.clear()
        actions.add(lavrado, golden, cheap)
        assertEquals(Money(3000 - 390), cart?.missingForMinOrder)
        assertFalse(cart!!.meetsMinOrder)
    }
}
