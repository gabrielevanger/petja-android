package io.github.gabrielevanger.xodopet.core.ui

import io.github.gabrielevanger.xodopet.core.domain.model.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FormattersTest {
    @Test
    fun `formats money in reais`() {
        assertEquals("R$ 0,00", Money(0).formatBrl())
        assertEquals("R$ 0,05", Money(5).formatBrl())
        assertEquals("R$ 5,00", Money(500).formatBrl())
        assertEquals("R$ 219,90", Money(21990).formatBrl())
        assertEquals("R$ 1.234,50", Money(123450).formatBrl())
        assertEquals("R$ 1.000.000,00", Money(100_000_000).formatBrl())
        assertEquals("-R$ 3,50", Money(-350).formatBrl())
    }

    @Test
    fun `formats distance`() {
        assertEquals("0 m", formatDistance(4))
        assertEquals("440 m", formatDistance(446))
        assertEquals("1,0 km", formatDistance(999 + 1))
        assertEquals("1,6 km", formatDistance(1613))
        assertEquals("9,9 km", formatDistance(9949))
        assertEquals("12 km", formatDistance(11_600))
    }

    @Test
    fun `contact links keep only digits`() {
        assertEquals("https://wa.me/5595900000001", whatsappLink("+55 (95) 90000-0001"))
        assertEquals("tel:9530000001", phoneLink("(95) 3000-0001"))
    }

    @Test
    fun `reads typed money`() {
        assertEquals(Money(10000), parseBrl("100"))
        assertEquals(Money(10050), parseBrl("100,5"))
        assertEquals(Money(10050), parseBrl(" R$ 100.50 "))
        assertEquals(Money(7), parseBrl("0,07"))
        assertEquals(null, parseBrl("1.000,00"))
        assertEquals(null, parseBrl("cem"))
        assertEquals(null, parseBrl(""))
    }

    @Test
    fun `date and time in Boa Vista`() {
        val instant = java.time.Instant.parse("2026-10-18T14:15:00Z")

        assertEquals(
            "18/10 às 10:15",
            formatDateTime(
                instant,
                "dd/MM 'às' HH:mm",
                java.time.ZoneId.of("America/Boa_Vista"),
                java.util.Locale.forLanguageTag("pt-BR"),
            ),
        )
    }
}
