package io.github.gabrielevanger.petja.core.data.remote

import io.github.gabrielevanger.petja.core.domain.checkout.CheckoutError
import io.github.gabrielevanger.petja.core.domain.checkout.CheckoutException
import io.github.gabrielevanger.petja.core.domain.checkout.PaymentChoice
import io.github.gabrielevanger.petja.core.domain.model.CartLine
import io.github.gabrielevanger.petja.core.domain.model.GeoPoint
import io.github.gabrielevanger.petja.core.domain.model.Money
import io.github.gabrielevanger.petja.core.domain.model.NewAddress
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
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SupabaseCheckoutRepositoryTest {
    private val requests = mutableListOf<Pair<HttpRequestData, String>>()
    private var respond: MockRequestHandleScope.() -> HttpResponseData = { json("[]") }

    private val repository = SupabaseCheckoutRepository(
        testSupabaseClient(
            MockEngine { request ->
                requests += request to String(request.body.toByteArray())
                respond()
            },
        ),
    )

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private val addressRow =
        """{"id":"a1","label":"Casa","street":"Rua Teste","number":"10","complement":null,"neighborhood":"Centro",
           "reference":"Perto da praça","lat":2.82,"lng":-60.672,"is_default":true}"""

    private val line = CartLine("1kg", "golden", "Ração Golden", "1 kg", Money(3090), quantity = 2, maxQuantity = 5)

    private fun body() = Json.parseToJsonElement(requests.last().second).jsonObject

    @Test
    fun `lists and adds addresses with coordinates`() = runTest {
        respond = { json("[$addressRow]") }

        val listed = repository.addresses().single()
        assertEquals("/rest/v1/rpc/my_addresses", requests.last().first.url.encodedPath)
        assertEquals(GeoPoint(2.82, -60.672), listed.location)

        val added = repository.addAddress(
            NewAddress("Rua Teste", "10", null, "Centro", "Perto da praça", GeoPoint(2.82, -60.672)),
        )
        assertEquals("/rest/v1/rpc/add_address", requests.last().first.url.encodedPath)
        assertEquals(-60.672, body().getValue("p_lng").jsonPrimitive.content.toDouble())
        assertEquals(JsonNull, body().getValue("p_complement"))
        assertEquals("a1", added.id)
    }

    @Test
    fun `places the order with items, payment and the expected total`() = runTest {
        respond = { json("""{"id":"o1","number":42,"total_cents":6680,"status":"pending","campo":"ignorado"}""") }

        val order = repository.placeOrder(
            "s1",
            "a1",
            listOf(line),
            PaymentChoice.Cash(Money(10000)),
            "Tocar",
            Money(6680),
        )

        assertEquals("/rest/v1/rpc/place_order", requests.last().first.url.encodedPath)
        val body = body()
        val item = body.getValue("p_items").jsonArray.single().jsonObject
        assertEquals("1kg", item.getValue("variant_id").jsonPrimitive.content)
        assertEquals(2, item.getValue("quantity").jsonPrimitive.int)
        assertEquals("cash", body.getValue("p_payment_method").jsonPrimitive.content)
        assertEquals(10000L, body.getValue("p_change_for_cents").jsonPrimitive.long)
        assertEquals(6680L, body.getValue("p_expected_total_cents").jsonPrimitive.long)
        assertEquals(42L, order.number)
        assertEquals(Money(6680), order.total)
    }

    @Test
    fun `pix and card on delivery have no change`() = runTest {
        respond = { json("""{"id":"o1","number":1,"total_cents":100}""") }

        repository.placeOrder("s1", "a1", listOf(line), PaymentChoice.PixOnDelivery, null, Money(100))
        assertEquals("pix_on_delivery", body().getValue("p_payment_method").jsonPrimitive.content)
        assertEquals(JsonNull, body().getValue("p_change_for_cents"))

        repository.placeOrder("s1", "a1", listOf(line), PaymentChoice.CardOnDelivery, null, Money(100))
        assertEquals("card_on_delivery", body().getValue("p_payment_method").jsonPrimitive.content)
    }

    @Test
    fun `database refusals become checkout errors`() = runTest {
        val cases = mapOf(
            "store_closed" to CheckoutError.StoreClosed,
            "out_of_delivery_area" to CheckoutError.OutOfDeliveryArea,
            "item_unavailable" to CheckoutError.ItemUnavailable,
            "insufficient_stock" to CheckoutError.InsufficientStock,
            "below_min_order" to CheckoutError.BelowMinOrder,
            "price_changed" to CheckoutError.PriceChanged,
            "invalid_change" to CheckoutError.ChangeTooLow,
            "empty_cart" to CheckoutError.EmptyCart,
            "not_authenticated" to CheckoutError.NotSignedIn,
            "address_not_found" to CheckoutError.Unknown,
        )
        cases.forEach { (code, expected) ->
            respond =
                {
                    json(
                        """{"code":"P0001","message":"$code","details":"texto","hint":null}""",
                        HttpStatusCode.BadRequest,
                    )
                }

            val exception = assertThrows<CheckoutException> {
                repository.placeOrder("s1", "a1", listOf(line), PaymentChoice.PixOnDelivery, null, Money(100))
            }
            assertEquals(expected, exception.error, code)
        }
    }

    @Test
    fun `signed out and network errors`() = runTest {
        respond =
            {
                json(
                    """{"code":"42501","message":"permission denied for function place_order"}""",
                    HttpStatusCode.Unauthorized,
                )
            }
        assertEquals(CheckoutError.NotSignedIn, assertThrows<CheckoutException> { repository.addresses() }.error)

        respond = { throw IOException("sem rede") }
        assertEquals(CheckoutError.Network, assertThrows<CheckoutException> { repository.addresses() }.error)
    }
}
