package io.github.gabrielevanger.xodopet.core.data.local

import io.github.gabrielevanger.xodopet.core.domain.model.DeliveryPlace
import io.github.gabrielevanger.xodopet.core.domain.model.GeoPoint
import kotlinx.serialization.Serializable

@Serializable
internal data class StoredDeliveryPlaceFile(val place: StoredDeliveryPlace? = null)

@Serializable
internal data class StoredDeliveryPlace(val name: String, val latitude: Double, val longitude: Double)

internal fun StoredDeliveryPlace.toDomain() = DeliveryPlace(name, GeoPoint(latitude, longitude))

internal fun DeliveryPlace.toStored() = StoredDeliveryPlace(name, point.latitude, point.longitude)
