package io.github.gabrielevanger.xodopet.core.domain.testing

import io.github.gabrielevanger.xodopet.core.domain.model.ExternalStore
import io.github.gabrielevanger.xodopet.core.domain.model.GeoPoint
import io.github.gabrielevanger.xodopet.core.domain.model.Money
import io.github.gabrielevanger.xodopet.core.domain.model.StoreSummary

val CENTRO = GeoPoint(2.8200, -60.6720)

fun aStore(
    id: String = "store-1",
    name: String = "Petshop Lavrado",
    isOpen: Boolean = true,
    distanceMeters: Int = 500,
    deliveryFeeCents: Long = 500,
    minOrderCents: Long = 3000,
) = StoreSummary(
    id = id,
    name = name,
    slug = name.lowercase().replace(' ', '-'),
    description = null,
    logoUrl = null,
    neighborhood = "Centro",
    whatsapp = "5595900000001",
    isOpen = isOpen,
    minOrder = Money(minOrderCents),
    estimatedMinutes = 40,
    distanceMeters = distanceMeters,
    deliveryFee = Money(deliveryFeeCents),
)

fun anExternalStore(
    id: String = "ext-1",
    name: String = "Agropet Satélite",
    distanceMeters: Int = 300,
    whatsapp: String? = "5595900001001",
    phone: String? = "(95) 3000-1001",
) = ExternalStore(
    id = id,
    name = name,
    phone = phone,
    whatsapp = whatsapp,
    street = "Rua Fictícia A, 10",
    neighborhood = "Cidade Satélite",
    location = GeoPoint(2.8620, -60.7580),
    distanceMeters = distanceMeters,
)
