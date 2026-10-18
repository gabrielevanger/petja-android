package io.github.gabrielevanger.xodopet.core.data.remote

import io.github.gabrielevanger.xodopet.core.domain.checkout.PaymentChoice
import io.github.gabrielevanger.xodopet.core.domain.model.Money
import io.github.gabrielevanger.xodopet.core.domain.model.OrderStatus
import io.github.gabrielevanger.xodopet.core.domain.storemode.ManagedStore
import io.github.gabrielevanger.xodopet.core.domain.storemode.StoreOrderError
import io.github.gabrielevanger.xodopet.core.domain.storemode.StoreOrderException
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SupabaseStoreOrderRepositoryTest {
    private val requests = mutableListOf<Pair<HttpRequestData, String>>()
    private var respond: MockRequestHandleScope.() -> HttpResponseData = { json("[]") }

    private val now = Instant.parse("2026-10-18T15:00:00Z")

    private val repository = SupabaseStoreOrderRepository(
        testSupabaseClient(
            MockEngine { request ->
                requests += request to String(request.body.toByteArray())
                respond()
            },
        ),
        Clock.fixed(now, ZoneOffset.UTC),
    )

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test
    fun `managed stores come from owner and staff memberships, once each, by name`() = runTest {
        respond = {
            json(
                """
                [{"store":{"id":"s2","name":"Petshop Lavrado"}},
                 {"store":{"id":"s1","name":"Agropet Caçari"}},
                 {"store":{"id":"s2","name":"Petshop Lavrado"}},
                 {"store":null}]
                """.trimIndent(),
            )
        }

        val stores = repository.managedStores()

        val request = requests.single().first
        assertEquals("/rest/v1/store_members", request.url.encodedPath)
        assertEquals("in.(owner,staff)", request.url.parameters["role"])
        assertEquals(listOf(ManagedStore("s1", "Agropet Caçari"), ManagedStore("s2", "Petshop Lavrado")), stores)
    }

    @Test
    fun `orders of the store, active or from the last day, newest first`() = runTest {
        respond = {
            json(
                """
                [{"id":"o1","number":7,"status":"out_for_delivery","status_reason":null,"customer_name":"Ana Souza",
                  "customer_phone":"","total_cents":6680,"payment_method":"cash","change_for_cents":10000,"notes":" ",
                  "street":"Rua Cerro Corá","number_address":"78","complement":"ap 2","neighborhood":"Centro","reference":null,
                  "created_at":"2026-10-18T14:15:00+00:00",
                  "items":[{"variant_id":"v1","product_name":"Ração Golden","variant_label":"1 kg","unit_price_cents":3090,"quantity":2}]},
                 {"id":"o2","number":6,"status":"rejected","status_reason":"Sem estoque","customer_name":"Bia",
                  "customer_phone":"95990000000","total_cents":500,"payment_method":"card_on_delivery",
                  "street":"Av. Ville Roy","number_address":"1200","neighborhood":"Caçari","created_at":"2026-10-18T09:00:00+00:00"}]
                """.trimIndent(),
            )
        }

        val orders = repository.orders("s1")

        val url = requests.single().first.url
        assertEquals("/rest/v1/orders", url.encodedPath)
        assertEquals("eq.s1", url.parameters["store_id"])
        val or = url.parameters["or"].orEmpty()
        assertTrue(or.contains("status.in.(pending,accepted,preparing,out_for_delivery)"), or)
        assertTrue(or.contains("created_at.gte.2026-10-17T15:00:00Z"), or)
        assertTrue(url.parameters["order"].orEmpty().startsWith("created_at.desc"))

        val first = orders[0]
        assertEquals(OrderStatus.OutForDelivery, first.status)
        assertEquals("Ana Souza", first.customerName)
        assertNull(first.customerPhone, "telefone vazio vira nulo")
        assertNull(first.notes, "observação em branco vira nula")
        assertEquals(PaymentChoice.Cash(Money(10000)), first.payment)
        assertEquals(2, first.items.single().quantity)
        assertEquals("ap 2", first.address.complement)
        assertEquals(OrderStatus.Rejected, orders[1].status)
        assertEquals("Sem estoque", orders[1].statusReason)
        assertEquals(PaymentChoice.CardOnDelivery, orders[1].payment)
    }

    @Test
    fun `transition sends the database status and the reason only when there is one`() = runTest {
        respond = { json("{}") }

        repository.transition("o1", OrderStatus.OutForDelivery, reason = null)
        val (request, body) = requests.last()
        assertEquals("/rest/v1/rpc/transition_order", request.url.encodedPath)
        assertTrue(body.contains("\"p_to\":\"out_for_delivery\""), body)
        assertFalse(body.contains("p_reason"), body)

        repository.transition("o1", OrderStatus.NotDelivered, reason = "Ninguém em casa")
        assertTrue(requests.last().second.contains("\"p_to\":\"not_delivered\""))
        assertTrue(requests.last().second.contains("\"p_reason\":\"Ninguém em casa\""))
    }

    @Test
    fun `server refusals become store order errors`() = runTest {
        fun refuse(message: String, code: String = "P0001", status: HttpStatusCode = HttpStatusCode.BadRequest) {
            respond = { json("""{"code":"$code","message":"$message"}""", status) }
        }
        suspend fun errorOf(block: suspend () -> Unit) = assertThrows<StoreOrderException> { block() }.error

        refuse("forbidden")
        assertEquals(StoreOrderError.NotAllowed, errorOf { repository.transition("o1", OrderStatus.Accepted, null) })
        refuse("invalid_transition")
        assertEquals(
            StoreOrderError.InvalidTransition,
            errorOf {
                repository.transition("o1", OrderStatus.Delivered, null)
            },
        )
        refuse("reason_required")
        assertEquals(
            StoreOrderError.ReasonRequired,
            errorOf {
                repository.transition("o1", OrderStatus.Rejected, null)
            },
        )
        refuse("not_authenticated")
        assertEquals(StoreOrderError.NotSignedIn, errorOf { repository.transition("o1", OrderStatus.Accepted, null) })
        refuse("permission denied", code = "42501", status = HttpStatusCode.Unauthorized)
        assertEquals(StoreOrderError.NotSignedIn, errorOf { repository.managedStores() })
        refuse("boom", code = "XX000", status = HttpStatusCode.InternalServerError)
        assertEquals(StoreOrderError.Unknown, errorOf { repository.orders("s1") })
        respond = { throw IOException("sem rede") }
        assertEquals(StoreOrderError.Network, errorOf { repository.orders("s1") })
    }

    @Test
    fun `status names match the database enum`() {
        assertEquals(
            listOf(
                "pending",
                "accepted",
                "preparing",
                "out_for_delivery",
                "delivered",
                "not_delivered",
                "rejected",
                "canceled",
            ),
            OrderStatus.entries.map { it.toDbValue() },
        )
        OrderStatus.entries.forEach { assertEquals(it, parseStatus(it.toDbValue())) }
    }
}
