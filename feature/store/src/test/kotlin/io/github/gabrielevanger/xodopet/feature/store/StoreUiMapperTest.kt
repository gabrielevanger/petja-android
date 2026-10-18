package io.github.gabrielevanger.xodopet.feature.store

import io.github.gabrielevanger.xodopet.core.domain.testing.aProduct
import io.github.gabrielevanger.xodopet.core.domain.testing.aStoreDetails
import io.github.gabrielevanger.xodopet.core.domain.testing.aVariant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StoreUiMapperTest {
    @Test
    fun `several prices show the lowest available as from`() {
        val row = aProduct(
            variants = listOf(
                aVariant(id = "a", priceCents = 990, stock = 0),
                aVariant(id = "b", priceCents = 3090),
                aVariant(id = "c", priceCents = 21990),
            ),
        ).toRow()

        assertEquals("R$ 30,90", row.price)
        assertTrue(row.fromPrice)
        assertTrue(row.isAvailable)
    }

    @Test
    fun `single price is shown without from`() {
        val row = aProduct(variants = listOf(aVariant(priceCents = 1290))).toRow()

        assertEquals("R$ 12,90", row.price)
        assertFalse(row.fromPrice)
    }

    @Test
    fun `nothing available has no price`() {
        val row = aProduct(variants = listOf(aVariant(stock = 0), aVariant(isActive = false))).toRow()

        assertNull(row.price)
        assertFalse(row.isAvailable)
    }

    @Test
    fun `detail keeps every variant with availability`() {
        val detail = aProduct(
            variants = listOf(aVariant(label = "1 kg"), aVariant(label = "15 kg", stock = 0)),
        ).toDetail()

        assertEquals(listOf(true, false), detail.variants.map { it.isAvailable })
    }

    @Test
    fun `header hides a zero minimum order`() {
        assertEquals("R$ 30,00", aStoreDetails().toHeader().minOrder)
        assertNull(
            aStoreDetails().copy(
                minOrder = io.github.gabrielevanger.xodopet.core.domain.model.Money.ZERO,
            ).toHeader().minOrder,
        )
    }
}
