package io.github.gabrielevanger.xodopet.core.domain.store

import io.github.gabrielevanger.xodopet.core.domain.model.Money
import io.github.gabrielevanger.xodopet.core.domain.testing.BRINQUEDOS
import io.github.gabrielevanger.xodopet.core.domain.testing.FakeStoreRepository
import io.github.gabrielevanger.xodopet.core.domain.testing.PETISCOS
import io.github.gabrielevanger.xodopet.core.domain.testing.RACAO
import io.github.gabrielevanger.xodopet.core.domain.testing.aProduct
import io.github.gabrielevanger.xodopet.core.domain.testing.aVariant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StoreCatalogTest {

    @Test
    fun `groups by category in the category order`() {
        val sections = groupByCategory(
            listOf(
                aProduct(id = "bola", category = BRINQUEDOS),
                aProduct(id = "golden", category = RACAO),
                aProduct(id = "bifinho", category = PETISCOS),
            ),
        )

        assertEquals(listOf("Ração", "Petiscos", "Brinquedos"), sections.map { it.category.name })
    }

    @Test
    fun `available products first, then by name`() {
        val unavailable = listOf(aVariant(stock = 0))
        val sections = groupByCategory(
            listOf(
                aProduct(id = "a", name = "Areia", variants = unavailable),
                aProduct(id = "c", name = "Coleira"),
                aProduct(id = "b", name = "Bifinho"),
            ),
        )

        assertEquals(listOf("Bifinho", "Coleira", "Areia"), sections.single().products.map { it.name })
    }

    @Test
    fun `variants follow the store order`() {
        val product = aProduct(
            variants = listOf(
                aVariant(id = "15", sortOrder = 2),
                aVariant(id = "1", sortOrder = 0),
                aVariant(id = "3", sortOrder = 1),
            ),
        )

        val sorted = groupByCategory(listOf(product)).single().products.single()

        assertEquals(listOf("1", "3", "15"), sorted.variants.map { it.id })
    }

    @Test
    fun `availability needs stock and an active variant`() {
        assertTrue(aVariant(stock = 1).isAvailable)
        assertFalse(aVariant(stock = 0).isAvailable)
        assertFalse(aVariant(stock = 5, isActive = false).isAvailable)
    }

    @Test
    fun `lowest price ignores unavailable variants`() {
        val product = aProduct(
            variants = listOf(
                aVariant(id = "barata-sem-estoque", priceCents = 990, stock = 0),
                aVariant(id = "media", priceCents = 2990),
                aVariant(id = "cara", priceCents = 21990),
            ),
        )

        assertEquals(Money(2990), product.lowestAvailablePrice)
        assertNull(aProduct(variants = listOf(aVariant(stock = 0))).lowestAvailablePrice)
    }

    @Test
    fun `loads store and grouped products`() = runTest {
        val catalog = LoadStoreCatalog(FakeStoreRepository())("store-1")

        assertEquals("Petshop Lavrado", catalog.store.name)
        assertEquals(1, catalog.sections.size)
    }

    @Test
    fun `unknown store is an error`() = runTest {
        assertThrows<StoreNotFoundException> { LoadStoreCatalog(FakeStoreRepository())("outra") }
    }
}
