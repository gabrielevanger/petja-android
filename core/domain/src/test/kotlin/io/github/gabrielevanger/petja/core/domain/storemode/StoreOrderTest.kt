package io.github.gabrielevanger.petja.core.domain.storemode

import io.github.gabrielevanger.petja.core.domain.model.OrderStatus
import io.github.gabrielevanger.petja.core.domain.testing.aStoreOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StoreOrderTest {

    @Test
    fun `each step offers the next move and the exceptions`() {
        val expected = mapOf(
            OrderStatus.Pending to listOf(StoreAction.Accept, StoreAction.Reject),
            OrderStatus.Accepted to listOf(StoreAction.StartPreparing),
            OrderStatus.Preparing to listOf(StoreAction.SendOut),
            OrderStatus.OutForDelivery to listOf(StoreAction.Deliver, StoreAction.NotDeliver),
        )
        OrderStatus.entries.forEach { status ->
            assertEquals(expected[status].orEmpty(), aStoreOrder(status = status).actions, status.name)
        }
    }

    @Test
    fun `only refusals ask for a reason`() {
        assertEquals(
            setOf(StoreAction.Reject, StoreAction.NotDeliver),
            StoreAction.entries.filter { it.needsReason }.toSet(),
        )
    }

    @Test
    fun `new orders are pending ones that were not there before`() {
        val old = aStoreOrder(id = "a")
        val accepted = aStoreOrder(id = "b", status = OrderStatus.Accepted)
        val fresh = aStoreOrder(id = "c")

        assertTrue(newOrders(before = null, after = listOf(old, fresh)).isEmpty(), "primeira carga não apita")
        assertEquals(listOf(fresh), newOrders(before = listOf(old), after = listOf(fresh, old, accepted)))
        assertTrue(newOrders(before = listOf(old, fresh), after = listOf(old, fresh)).isEmpty())
    }
}
