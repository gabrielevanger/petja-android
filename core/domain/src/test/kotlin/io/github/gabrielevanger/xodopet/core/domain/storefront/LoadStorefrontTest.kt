package io.github.gabrielevanger.xodopet.core.domain.storefront

import io.github.gabrielevanger.xodopet.core.domain.testing.CENTRO
import io.github.gabrielevanger.xodopet.core.domain.testing.FakeStorefrontRepository
import io.github.gabrielevanger.xodopet.core.domain.testing.aStore
import io.github.gabrielevanger.xodopet.core.domain.testing.anExternalStore
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LoadStorefrontTest {
    private val repository = FakeStorefrontRepository()
    private val loadStorefront = LoadStorefront(repository)

    @Test
    fun `with an open partner, does not look for non partners`() = runTest {
        repository.stores = listOf(aStore())
        repository.externalStores = listOf(anExternalStore())

        val storefront = loadStorefront(CENTRO)

        assertEquals(1, storefront.partners.size)
        assertTrue(storefront.nonPartners.isEmpty())
        assertTrue(repository.externalStoresNearCalls.isEmpty())
        assertTrue(storefront.hasOpenPartner)
    }

    @Test
    fun `without partners in range, shows non partners`() = runTest {
        repository.externalStores = listOf(anExternalStore())

        val storefront = loadStorefront(CENTRO)

        assertTrue(storefront.partners.isEmpty())
        assertEquals(listOf("Agropet Satélite"), storefront.nonPartners.map { it.name })
        assertEquals(listOf(CENTRO), repository.externalStoresNearCalls)
    }

    @Test
    fun `with every partner closed, keeps them and also shows non partners`() = runTest {
        repository.stores = listOf(aStore(isOpen = false))
        repository.externalStores = listOf(anExternalStore())

        val storefront = loadStorefront(CENTRO)

        assertEquals(1, storefront.partners.size)
        assertEquals(1, storefront.nonPartners.size)
        assertFalse(storefront.hasOpenPartner)
    }

    @Test
    fun `orders partners open first, then by distance`() = runTest {
        repository.stores = listOf(
            aStore(id = "closed-near", isOpen = false, distanceMeters = 100),
            aStore(id = "open-far", distanceMeters = 3000),
            aStore(id = "open-near", distanceMeters = 800),
        )

        val storefront = loadStorefront(CENTRO)

        assertEquals(listOf("open-near", "open-far", "closed-near"), storefront.partners.map { it.id })
    }

    @Test
    fun `orders non partners by distance`() = runTest {
        repository.externalStores = listOf(
            anExternalStore(id = "far", distanceMeters = 7000),
            anExternalStore(id = "near", distanceMeters = 300),
        )

        val storefront = loadStorefront(CENTRO)

        assertEquals(listOf("near", "far"), storefront.nonPartners.map { it.id })
    }
}
