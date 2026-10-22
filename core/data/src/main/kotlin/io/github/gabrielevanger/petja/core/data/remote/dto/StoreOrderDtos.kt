package io.github.gabrielevanger.petja.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ManagedStoreDto(val id: String, val name: String)

/** Participação na equipe, com a loja embutida; a RLS só mostra as lojas que a conta pode ver. */
@Serializable
internal data class StoreMembershipDto(val store: ManagedStoreDto? = null)

@Serializable
internal data class StoreOrderDto(
    val id: String,
    val number: Long,
    val status: String,
    @SerialName("status_reason") val statusReason: String? = null,
    @SerialName("customer_name") val customerName: String,
    @SerialName("customer_phone") val customerPhone: String? = null,
    @SerialName("total_cents") val totalCents: Long,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("change_for_cents") val changeForCents: Long? = null,
    val notes: String? = null,
    val street: String,
    @SerialName("number_address") val numberAddress: String,
    val complement: String? = null,
    val neighborhood: String,
    val reference: String? = null,
    @SerialName("created_at") val createdAt: String,
    val items: List<OrderItemDto> = emptyList(),
)

internal const val STORE_ORDER_COLUMNS =
    "id,number,status,status_reason,customer_name,customer_phone,total_cents,payment_method,change_for_cents," +
        "notes,street,number_address,complement,neighborhood,reference,created_at," +
        "items:order_items(variant_id,product_name,variant_label,unit_price_cents,quantity)"
