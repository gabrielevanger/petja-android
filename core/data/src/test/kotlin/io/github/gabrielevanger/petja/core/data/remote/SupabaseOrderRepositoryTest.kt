package io.github.gabrielevanger.petja.core.data.remote

import io.github.gabrielevanger.petja.core.domain.checkout.PaymentChoice
import io.github.gabrielevanger.petja.core.domain.model.Money
import io.github.gabrielevanger.petja.core.domain.model.OrderStatus
import io.github.gabrielevanger.petja.core.domain.orders.OrderError
import io.github.gabrielevanger.petja.core.domain.orders.OrderException
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SupabaseOrderRepositoryTest {
    private val requests = mutableListOf<Pair<HttpRequestData, String>>()
    private var respond: MockRequestHandleScope.() -> HttpResponseData = { json("[]") }

    private val repository = SupabaseOrderRepository(
        testSupabaseClient(
            MockEngine { request ->
                requests += request to String(request.body.toByteArray())
                respond()
            },
        ),
    )

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test
    fun `my orders, newest first, with store and item count`() = runTest {
        respond = {
            json(
                """
                [{"id":"o2","number":2,"status":"out_for_delivery","total_cents":7680,"created_at":"2026-10-18T14:15:00.123456+00:00",
                  "store":{"name":"Petshop Lavrado"},"items":[{"quantity":2},{"quantity":1}]},
                 {"id":"o1","number":1,"status":"status_novo","total_cents":500,"created_at":"2026-10-17T10:00:00+00:00",
                  "store":null,"items":[]}]
                """.trimIndent(),
            )
        }

        val orders = repository.myOrders()

        val request = requests.single().first
        assertEquals("/rest/v1/orders", request.url.encodedPath)
        assertTrue(request.url.parameters["order"].orEmpty().startsWith("created_at.desc"))
        assertTrue(request.url.parameters["select"].orEmpty().contains("store:stores(name)"))
        assertEquals(OrderStatus.OutForDelivery, orders[0].status)
        assertEquals(3, orders[0].itemCount)
        assertEquals(Instant.parse("2026-10-18T14:15:00.123456Z"), orders[0].createdAt)
        assertEquals(OrderStatus.Pending, orders[1].status, "status desconhecido não derruba a lista")
        assertEquals("", orders[1].storeName)
    }

    @Test
    fun `order details with items, payment, address and history in order`() = runTest {
        respond = {
            json(
                """
                [{"id":"o1","number":1,"status":"canceled","status_reason":"A loja não respondeu a tempo","store_id":"s1",
                  "subtotal_cents":7180,"delivery_fee_cents":500,"total_cents":7680,"payment_method":"cash","change_for_cents":10000,
                  "notes":null,"street":"Rua Cerro Corá","number_address":"78","complement":null,"neighborhood":"Centro",
                  "reference":"Praça","courier_name":null,"created_at":"2026-10-18T14:15:00+00:00",
                  "store":{"name":"Petshop Lavrado","whatsapp":"5595900000001"},
                  "items":[{"variant_id":"v1","product_name":"Ração Golden","variant_label":"1 kg","unit_price_cents":3090,"quantity":1}],
                  "history":[{"to_status":"canceled","reason":"A loja não respondeu a tempo","created_at":"2026-10-18T14:26:00+00:00"},
                             {"to_status":"pending","reason":null,"created_at":"2026-10-18T14:15:00+00:00"}]}]
                """.trimIndent(),
            )
        }

        val order = repository.order("o1")!!

        assertEquals("eq.o1", requests.single().first.url.parameters["id"])
        assertEquals(OrderStatus.Canceled, order.status)
        assertEquals(PaymentChoice.Cash(Money(10000)), order.payment)
        assertEquals("Ração Golden", order.items.single().productName)
        assertEquals("78", order.address.number)
        assertEquals(listOf(OrderStatus.Pending, OrderStatus.Canceled), order.history.map { it.status })
        assertEquals("5595900000001", order.storeWhatsapp)
    }

    @Test
    fun `pix, card and missing order`() = runTest {
        val row = { method: String ->
            """[{"id":"o1","number":1,"status":"pending","store_id":"s1","subtotal_cents":1,"delivery_fee_cents":0,"total_cents":1,
                "payment_method":"$method","street":"R","number_address":"1","neighborhood":"B","created_at":"2026-10-18T14:15:00+00:00"}]"""
        }
        respond = { json(row("pix_on_delivery")) }
        assertEquals(PaymentChoice.PixOnDelivery, repository.order("o1")!!.payment)
        respond = { json(row("card_on_delivery")) }
        assertEquals(PaymentChoice.CardOnDelivery, repository.order("o1")!!.payment)
        respond = { json("[]") }
        assertNull(repository.order("outro"))
    }

    @Test
    fun `cancel calls transition_order and explains refusals`() = runTest {
        respond = { json("{}") }
        repository.cancel("o1")
        assertEquals("/rest/v1/rpc/transition_order", requests.last().first.url.encodedPath)
        assertTrue(requests.last().second.contains("\"p_to\":\"canceled\""))

        respond = { json("""{"code":"P0001","message":"forbidden"}""", HttpStatusCode.BadRequest) }
        assertEquals(OrderError.CannotCancel, assertThrows<OrderException> { repository.cancel("o1") }.error)

        respond = { json("""{"code":"42501","message":"permission denied"}""", HttpStatusCode.Unauthorized) }
        assertEquals(OrderError.NotSignedIn, assertThrows<OrderException> { repository.myOrders() }.error)

        respond = { json("""{"code":"XX000","message":"boom"}""", HttpStatusCode.InternalServerError) }
        assertEquals(OrderError.Unknown, assertThrows<OrderException> { repository.myOrders() }.error)

        respond = { throw IOException("sem rede") }
        assertEquals(OrderError.Network, assertThrows<OrderException> { repository.myOrders() }.error)
    }
}
