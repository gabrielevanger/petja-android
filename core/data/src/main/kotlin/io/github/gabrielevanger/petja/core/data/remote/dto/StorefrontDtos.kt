package io.github.gabrielevanger.petja.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Linha devolvida pela função stores_near (supabase/migrations/..._functions.sql). */
@Serializable
internal data class StoreNearDto(
    val id: String,
    val name: String,
    val slug: String,
    val description: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    val neighborhood: String,
    val whatsapp: String? = null,
    @SerialName("is_open") val isOpen: Boolean,
    @SerialName("min_order_cents") val minOrderCents: Long,
    @SerialName("estimated_minutes") val estimatedMinutes: Int,
    @SerialName("distance_m") val distanceMeters: Int,
    @SerialName("delivery_fee_cents") val deliveryFeeCents: Long,
)

/** Linha devolvida pela função external_stores_near. */
@Serializable
internal data class ExternalStoreDto(
    val id: String,
    val name: String,
    val phone: String? = null,
    val whatsapp: String? = null,
    val street: String? = null,
    val neighborhood: String? = null,
    val lat: Double,
    val lng: Double,
    @SerialName("distance_m") val distanceMeters: Int,
)
