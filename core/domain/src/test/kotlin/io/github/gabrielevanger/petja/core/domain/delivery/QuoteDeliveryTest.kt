package io.github.gabrielevanger.petja.core.domain.delivery

import io.github.gabrielevanger.petja.core.domain.model.Money
import io.github.gabrielevanger.petja.core.domain.testing.CENTRO
import io.github.gabrielevanger.petja.core.domain.testing.FakeStorefrontRepository
import io.github.gabrielevanger.petja.core.domain.testing.aStore
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class QuoteDeliveryTest {
    private val storefront =
        FakeStorefrontRepository(stores = listOf(aStore(id = "lavrado", deliveryFeeCents = 650, distanceMeters = 1613)))

    @Test
    fun `quote comes from the store in range`() = runTest {
        val quote = QuoteDelivery(storefront)("lavrado", CENTRO)

        assertEquals(DeliveryQuote(Money(650), 1613, storeIsOpen = true), quote)
    }

    @Test
    fun `store out of range has no quote`() = runTest {
        assertNull(QuoteDelivery(storefront)("outra", CENTRO))
    }
}
