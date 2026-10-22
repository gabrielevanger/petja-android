package io.github.gabrielevanger.petja.core.data.remote

import io.github.gabrielevanger.petja.core.data.remote.dto.AddressDto
import io.github.gabrielevanger.petja.core.data.remote.dto.PlacedOrderDto
import io.github.gabrielevanger.petja.core.domain.checkout.CheckoutError
import io.github.gabrielevanger.petja.core.domain.checkout.CheckoutException
import io.github.gabrielevanger.petja.core.domain.checkout.CheckoutRepository
import io.github.gabrielevanger.petja.core.domain.checkout.PaymentChoice
import io.github.gabrielevanger.petja.core.domain.checkout.PlacedOrder
import io.github.gabrielevanger.petja.core.domain.model.Address
import io.github.gabrielevanger.petja.core.domain.model.CartLine
import io.github.gabrielevanger.petja.core.domain.model.GeoPoint
import io.github.gabrielevanger.petja.core.domain.model.Money
import io.github.gabrielevanger.petja.core.domain.model.NewAddress
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.ktor.client.plugins.HttpRequestTimeoutException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** SQLSTATE de "sem permissão": a função de pedido só aceita usuário logado. */
private const val PERMISSION_DENIED = "42501"

/** Endereços e pedido pelas funções do banco (supabase/migrations), que validam tudo no servidor. */
internal class SupabaseCheckoutRepository @Inject constructor(private val supabase: SupabaseClient) :
    CheckoutRepository {

    override suspend fun addresses(): List<Address> = translateErrors {
        supabase.postgrest.rpc("my_addresses").decodeList<AddressDto>().map { it.toDomain() }
    }

    override suspend fun addAddress(address: NewAddress): Address = translateErrors {
        val parameters = buildJsonObject {
            put("p_street", address.street)
            put("p_number", address.number)
            put("p_neighborhood", address.neighborhood)
            put("p_lat", address.location.latitude)
            put("p_lng", address.location.longitude)
            put("p_complement", address.complement)
            put("p_reference", address.reference)
        }
        supabase.postgrest.rpc("add_address", parameters).decodeList<AddressDto>().single().toDomain()
    }

    override suspend fun placeOrder(
        storeId: String,
        addressId: String,
        lines: List<CartLine>,
        payment: PaymentChoice,
        notes: String?,
        expectedTotal: Money,
    ): PlacedOrder = translateErrors {
        supabase.postgrest
            .rpc("place_order", orderParameters(storeId, addressId, lines, payment, notes, expectedTotal))
            .decodeAs<PlacedOrderDto>()
            .let { PlacedOrder(id = it.id, number = it.number, total = Money(it.totalCents)) }
    }

    private fun orderParameters(
        storeId: String,
        addressId: String,
        lines: List<CartLine>,
        payment: PaymentChoice,
        notes: String?,
        expectedTotal: Money,
    ): JsonObject = buildJsonObject {
        put("p_store_id", storeId)
        put("p_address_id", addressId)
        putJsonArray("p_items") {
            lines.forEach { line ->
                addJsonObject {
                    put("variant_id", line.variantId)
                    put("quantity", line.quantity)
                }
            }
        }
        put(
            "p_payment_method",
            when (payment) {
                is PaymentChoice.Cash -> "cash"
                PaymentChoice.PixOnDelivery -> "pix_on_delivery"
                PaymentChoice.CardOnDelivery -> "card_on_delivery"
            },
        )
        put("p_change_for_cents", (payment as? PaymentChoice.Cash)?.changeFor?.cents)
        put("p_notes", notes)
        put("p_expected_total_cents", expectedTotal.cents)
    }

    private fun AddressDto.toDomain() = Address(
        id = id,
        label = label,
        street = street,
        number = number,
        complement = complement,
        neighborhood = neighborhood,
        reference = reference,
        location = GeoPoint(lat, lng),
        isDefault = isDefault,
    )

    private suspend fun <T> translateErrors(block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: PostgrestRestException) {
        throw CheckoutException(
            if (e.code ==
                PERMISSION_DENIED
            ) {
                CheckoutError.NotSignedIn
            } else {
                e.error.toCheckoutError()
            },
            e,
        )
    } catch (e: HttpRequestException) {
        throw CheckoutException(CheckoutError.Network, e)
    } catch (e: HttpRequestTimeoutException) {
        throw CheckoutException(CheckoutError.Network, e)
    } catch (e: Exception) {
        throw CheckoutException(CheckoutError.Unknown, e)
    }

    /** Os códigos são as mensagens dos `raise exception` de place_order (migração de funções). */
    private fun String.toCheckoutError() = when (this) {
        "not_authenticated" -> CheckoutError.NotSignedIn
        "store_closed" -> CheckoutError.StoreClosed
        "out_of_delivery_area" -> CheckoutError.OutOfDeliveryArea
        "item_unavailable" -> CheckoutError.ItemUnavailable
        "insufficient_stock" -> CheckoutError.InsufficientStock
        "below_min_order" -> CheckoutError.BelowMinOrder
        "price_changed" -> CheckoutError.PriceChanged
        "invalid_change" -> CheckoutError.ChangeTooLow
        "empty_cart" -> CheckoutError.EmptyCart
        else -> CheckoutError.Unknown
    }
}
