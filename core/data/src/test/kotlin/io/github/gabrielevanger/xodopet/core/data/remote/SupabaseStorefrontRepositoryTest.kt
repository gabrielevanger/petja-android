package io.github.gabrielevanger.xodopet.core.data.remote

import io.github.gabrielevanger.xodopet.core.domain.model.GeoPoint
import io.github.gabrielevanger.xodopet.core.domain.model.Money
import io.github.gabrielevanger.xodopet.core.domain.testing.CENTRO
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SupabaseStorefrontRepositoryTest {
    private val requests = mutableListOf<Pair<HttpRequestData, String>>()

    private fun repository(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData,
    ) = SupabaseStorefrontRepository(
        testSupabaseClient(
            MockEngine { request ->
                requests += request to String(request.body.toByteArray())
                handler(request)
            },
        ),
    )

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test
    fun `storesNear calls the stores_near function with the point and maps the rows`() = runTest {
        val repository = repository {
            json(
                """
                [{"id":"00000000-0000-4000-a000-000000000001","name":"Petshop Lavrado","slug":"petshop-lavrado",
                  "description":"","logo_url":null,"neighborhood":"Centro","whatsapp":"5595900000001",
                  "is_open":true,"min_order_cents":3000,"estimated_minutes":40,"distance_m":446,
                  "delivery_fee_cents":500,"campo_novo":"ignorado"}]
                """.trimIndent(),
            )
        }

        val stores = repository.storesNear(CENTRO)

        val (request, body) = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/rest/v1/rpc/stores_near", request.url.encodedPath)
        assertEquals("chave-anon", request.headers["apikey"])
        val params = Json.parseToJsonElement(body).jsonObject
        assertEquals(CENTRO.latitude, params.getValue("p_lat").jsonPrimitive.double)
        assertEquals(CENTRO.longitude, params.getValue("p_lng").jsonPrimitive.double)

        val store = stores.single()
        assertEquals("Petshop Lavrado", store.name)
        assertEquals(Money(3000), store.minOrder)
        assertEquals(Money(500), store.deliveryFee)
        assertEquals(446, store.distanceMeters)
        assertNull(store.description, "descrição vazia vira null")
    }

    @Test
    fun `externalStoresNear maps coordinates`() = runTest {
        val repository = repository {
            json(
                """
                [{"id":"e1","name":"Agropet Satélite","phone":"(95) 3000-1001","whatsapp":null,
                  "street":"Rua Fictícia A, 10","neighborhood":"Cidade Satélite",
                  "lat":2.862,"lng":-60.758,"distance_m":314}]
                """.trimIndent(),
            )
        }

        val store = repository.externalStoresNear(GeoPoint(2.86, -60.76)).single()

        assertEquals("/rest/v1/rpc/external_stores_near", requests.single().first.url.encodedPath)
        assertEquals(GeoPoint(2.862, -60.758), store.location)
        assertNull(store.whatsapp)
    }

    @Test
    fun `server error is thrown`() = runTest {
        val repository = repository {
            json("""{"message":"boom","code":"XX000"}""", HttpStatusCode.InternalServerError)
        }

        assertThrows<Exception> { repository.storesNear(CENTRO) }
    }
}
