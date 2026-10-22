package io.github.gabrielevanger.petja.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Linha devolvida por my_addresses() e add_address(). */
@Serializable
internal data class AddressDto(
    val id: String,
    val label: String? = null,
    val street: String,
    val number: String,
    val complement: String? = null,
    val neighborhood: String,
    val reference: String? = null,
    val lat: Double,
    val lng: Double,
    @SerialName("is_default") val isDefault: Boolean,
)

/** Só as colunas do pedido criado que o app usa; o resto da linha de orders é ignorado. */
@Serializable
internal data class PlacedOrderDto(val id: String, val number: Long, @SerialName("total_cents") val totalCents: Long)
