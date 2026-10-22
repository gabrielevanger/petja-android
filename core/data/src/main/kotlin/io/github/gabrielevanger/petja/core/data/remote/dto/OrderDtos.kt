package io.github.gabrielevanger.petja.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OrderStoreDto(val name: String, val whatsapp: String? = null)

@Serializable
internal data class OrderItemCountDto(val quantity: Int)

/** Linha de "Meus pedidos", com o nome da loja e as quantidades dos itens embutidos. */
@Serializable
internal data class OrderSummaryDto(
    val id: String,
    val number: Long,
    val status: String,
    @SerialName("total_cents") val totalCents: Long,
    @SerialName("created_at") val createdAt: String,
    // A loja some do embed se for desativada depois (RLS); o pedido continua na lista.
    val store: OrderStoreDto? = null,
    val items: List<OrderItemCountDto> = emptyList(),
)

@Serializable
internal data class OrderItemDto(
    @SerialName("variant_id") val variantId: String? = null,
    @SerialName("product_name") val productName: String,
    @SerialName("variant_label") val variantLabel: String,
    @SerialName("unit_price_cents") val unitPriceCents: Long,
    val quantity: Int,
)

@Serializable
internal data class StatusChangeDto(
    @SerialName("to_status") val toStatus: String,
    val reason: String? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
internal data class OrderDetailsDto(
    val id: String,
    val number: Long,
    val status: String,
    @SerialName("status_reason") val statusReason: String? = null,
    @SerialName("store_id") val storeId: String,
    @SerialName("subtotal_cents") val subtotalCents: Long,
    @SerialName("delivery_fee_cents") val deliveryFeeCents: Long,
    @SerialName("total_cents") val totalCents: Long,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("change_for_cents") val changeForCents: Long? = null,
    val notes: String? = null,
    val street: String,
    @SerialName("number_address") val numberAddress: String,
    val complement: String? = null,
    val neighborhood: String,
    val reference: String? = null,
    @SerialName("courier_name") val courierName: String? = null,
    @SerialName("created_at") val createdAt: String,
    val store: OrderStoreDto? = null,
    val items: List<OrderItemDto> = emptyList(),
    val history: List<StatusChangeDto> = emptyList(),
)

internal const val ORDER_SUMMARY_COLUMNS =
    "id,number,status,total_cents,created_at,store:stores(name),items:order_items(quantity)"

internal const val ORDER_DETAILS_COLUMNS =
    "id,number,status,status_reason,store_id,subtotal_cents,delivery_fee_cents,total_cents,payment_method," +
        "change_for_cents,notes,street,number_address,complement,neighborhood,reference,courier_name,created_at," +
        "store:stores(name,whatsapp)," +
        "items:order_items(variant_id,product_name,variant_label,unit_price_cents,quantity)," +
        "history:order_status_history(to_status,reason,created_at)"
