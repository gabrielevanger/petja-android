package io.github.gabrielevanger.xodopet.feature.storefront

import io.github.gabrielevanger.xodopet.core.domain.testing.aStore
import io.github.gabrielevanger.xodopet.core.domain.testing.anExternalStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StorefrontUiMapperTest {
    @Test
    fun `formats store values`() {
        val card = aStore(distanceMeters = 1613, deliveryFeeCents = 650, minOrderCents = 3000).toCard()

        assertEquals("R$ 6,50", card.deliveryFee)
        assertFalse(card.isFreeDelivery)
        assertEquals("R$ 30,00", card.minOrder)
        assertEquals("1,6 km", card.distance)
    }

    @Test
    fun `free delivery and no minimum`() {
        val card = aStore(deliveryFeeCents = 0, minOrderCents = 0).toCard()

        assertTrue(card.isFreeDelivery)
        assertNull(card.minOrder)
    }

    @Test
    fun `external store links`() {
        val card = anExternalStore(whatsapp = "5595900001001", phone = null).toCard()

        assertEquals("https://wa.me/5595900001001", card.whatsappLink)
        assertNull(card.phoneLink)
        assertEquals("Rua Fictícia A, 10 · Cidade Satélite", card.address)
    }
}
