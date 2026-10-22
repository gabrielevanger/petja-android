package io.github.gabrielevanger.petja.core.domain.orders

import io.github.gabrielevanger.petja.core.domain.model.Money
import io.github.gabrielevanger.petja.core.domain.model.OrderItem
import io.github.gabrielevanger.petja.core.domain.model.OrderStatus
import io.github.gabrielevanger.petja.core.domain.store.StoreNotFoundException
import io.github.gabrielevanger.petja.core.domain.testing.FakeCartRepository
import io.github.gabrielevanger.petja.core.domain.testing.FakeStoreRepository
import io.github.gabrielevanger.petja.core.domain.testing.aProduct
import io.github.gabrielevanger.petja.core.domain.testing.aVariant
import io.github.gabrielevanger.petja.core.domain.testing.anOrderDetails
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OrdersTest {
    private val carts = FakeCartRepository()
    private val stores = FakeStoreRepository(
        products = listOf(
            aProduct(
                id = "golden",
                name = "Ração Golden",
                variants = listOf(aVariant(id = "1kg", priceCents = 3290, stock = 5)),
            ),
            aProduct(id = "granplus", name = "Ração GranPlus", variants = listOf(aVariant(id = "granplus", stock = 0))),
        ),
    )
    private val reorder = ReorderToCart(stores, carts)

    @Test
    fun `final states and who can cancel`() {
        assertTrue(OrderStatus.Pending.customerCanCancel)
        assertFalse(OrderStatus.Accepted.customerCanCancel)
        assertEquals(
            setOf(OrderStatus.Delivered, OrderStatus.NotDelivered, OrderStatus.Rejected, OrderStatus.Canceled),
            OrderStatus.entries.filter { it.isFinal }.toSet(),
        )
        assertEquals(Money(6180), OrderItem("v", "Ração", "1 kg", Money(3090), quantity = 2).total)
    }

    @Test
    fun `reorder brings back what is available with today's price`() = runTest {
        val result = reorder(anOrderDetails())

        assertEquals(ReorderResult.SomeChanged(missing = 1), result)
        val line = carts.cart.value!!.lines.single()
        assertEquals("1kg", line.variantId)
        assertEquals(Money(3290), line.unitPrice, "preço de agora, não o do pedido")
        assertEquals("Petshop Lavrado", carts.cart.value!!.storeName)
    }

    @Test
    fun `reorder limits the quantity to the stock`() = runTest {
        val order = anOrderDetails(items = listOf(OrderItem("1kg", "Ração Golden", "1 kg", Money(3090), quantity = 9)))

        assertEquals(ReorderResult.SomeChanged(missing = 1), reorder(order))
        assertEquals(5, carts.cart.value!!.lines.single().quantity)
    }

    @Test
    fun `reorder with everything available`() = runTest {
        val order = anOrderDetails(items = listOf(OrderItem("1kg", "Ração Golden", "1 kg", Money(3090), quantity = 2)))

        assertEquals(ReorderResult.AllAdded, reorder(order))
    }

    @Test
    fun `nothing available keeps the current cart`() = runTest {
        val order =
            anOrderDetails(items = listOf(OrderItem("granplus", "Ração GranPlus", "1 kg", Money(4090), quantity = 1)))

        assertEquals(ReorderResult.NothingAvailable, reorder(order))
        assertNull(carts.cart.value)
    }

    @Test
    fun `store gone`() = runTest {
        assertThrows<StoreNotFoundException> { reorder(anOrderDetails().copy(storeId = "fechou")) }
        assertEquals("NotSignedIn", OrderException(OrderError.NotSignedIn).message)
    }
}
