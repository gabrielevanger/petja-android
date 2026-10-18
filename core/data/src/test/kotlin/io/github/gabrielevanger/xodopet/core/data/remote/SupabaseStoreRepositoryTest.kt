package io.github.gabrielevanger.xodopet.core.data.remote

import io.github.gabrielevanger.xodopet.core.domain.model.Money
import io.github.gabrielevanger.xodopet.core.domain.model.PetSpecies
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SupabaseStoreRepositoryTest {
    private val requests = mutableListOf<HttpRequestData>()

    private fun repository(body: String) = SupabaseStoreRepository(
        testSupabaseClient(
            MockEngine { request ->
                requests += request
                respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
            },
        ),
    )

    @Test
    fun `products asks for active products of the store with category and variants embedded`() = runTest {
        val repository = repository(
            """
            [{"id":"p1","name":"Ração Golden","description":"","brand":"Golden","species":["dog","dinossauro"],
              "image_urls":[],"category":{"id":1,"name":"Ração","slug":"racao","sort_order":1},
              "variants":[{"id":"v1","label":"1 kg","price_cents":3090,"stock":2,"is_active":true,"sort_order":0}]}]
            """.trimIndent(),
        )

        val product = repository.products("store-1").single()

        val request = requests.single()
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("/rest/v1/products", request.url.encodedPath)
        assertEquals("eq.store-1", request.url.parameters["store_id"])
        assertEquals("eq.true", request.url.parameters["is_active"])
        val select = request.url.parameters["select"].orEmpty()
        assertTrue(select.contains("category:categories(") && select.contains("variants:product_variants("), select)

        assertEquals("Ração", product.category.name)
        assertEquals(Money(3090), product.variants.single().price)
        assertNull(product.description, "descrição vazia vira null")
        assertEquals(setOf(PetSpecies.DOG, PetSpecies.OTHER), product.species, "espécie desconhecida vira OTHER")
    }

    @Test
    fun `store returns null when the table answers empty`() = runTest {
        val repository = repository("[]")

        assertNull(repository.store("sumiu"))
        assertEquals("eq.sumiu", requests.single().url.parameters["id"])
    }

    @Test
    fun `store maps the row`() = runTest {
        val repository = repository(
            """
            [{"id":"s1","name":"Petshop Lavrado","description":"Centro","neighborhood":"Centro",
              "whatsapp":"5595900000001","is_open":false,"min_order_cents":3000,"estimated_minutes":40}]
            """.trimIndent(),
        )

        val store = repository.store("s1")!!

        assertEquals("Petshop Lavrado", store.name)
        assertEquals(false, store.isOpen)
        assertEquals(Money(3000), store.minOrder)
    }
}
