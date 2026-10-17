package io.github.gabrielevanger.xodopet.core.data.remote

import io.github.gabrielevanger.xodopet.core.data.remote.dto.ExternalStoreDto
import io.github.gabrielevanger.xodopet.core.data.remote.dto.StoreNearDto
import io.github.gabrielevanger.xodopet.core.domain.model.ExternalStore
import io.github.gabrielevanger.xodopet.core.domain.model.GeoPoint
import io.github.gabrielevanger.xodopet.core.domain.model.Money
import io.github.gabrielevanger.xodopet.core.domain.model.StoreSummary

internal fun StoreNearDto.toDomain() = StoreSummary(
    id = id,
    name = name,
    slug = slug,
    description = description?.takeIf { it.isNotBlank() },
    logoUrl = logoUrl?.takeIf { it.isNotBlank() },
    neighborhood = neighborhood,
    whatsapp = whatsapp?.takeIf { it.isNotBlank() },
    isOpen = isOpen,
    minOrder = Money(minOrderCents),
    estimatedMinutes = estimatedMinutes,
    distanceMeters = distanceMeters,
    deliveryFee = Money(deliveryFeeCents),
)

internal fun ExternalStoreDto.toDomain() = ExternalStore(
    id = id,
    name = name,
    phone = phone?.takeIf { it.isNotBlank() },
    whatsapp = whatsapp?.takeIf { it.isNotBlank() },
    street = street,
    neighborhood = neighborhood,
    location = GeoPoint(lat, lng),
    distanceMeters = distanceMeters,
)
