package io.github.gabrielevanger.xodopet.feature.storefront

import io.github.gabrielevanger.xodopet.core.domain.model.ExternalStore
import io.github.gabrielevanger.xodopet.core.domain.model.Money
import io.github.gabrielevanger.xodopet.core.domain.model.StoreSummary
import io.github.gabrielevanger.xodopet.core.domain.storefront.Storefront
import io.github.gabrielevanger.xodopet.core.ui.formatBrl
import io.github.gabrielevanger.xodopet.core.ui.formatDistance
import io.github.gabrielevanger.xodopet.core.ui.phoneLink
import io.github.gabrielevanger.xodopet.core.ui.whatsappLink
import kotlinx.collections.immutable.toImmutableList

internal fun Storefront.toContent() = StorefrontContent.Loaded(
    partners = partners.map { it.toCard() }.toImmutableList(),
    nonPartners = nonPartners.map { it.toCard() }.toImmutableList(),
)

internal fun StoreSummary.toCard() = StoreCardUi(
    id = id,
    name = name,
    neighborhood = neighborhood,
    isOpen = isOpen,
    estimatedMinutes = estimatedMinutes,
    deliveryFee = deliveryFee.formatBrl(),
    isFreeDelivery = deliveryFee == Money.ZERO,
    minOrder = minOrder.takeIf { it > Money.ZERO }?.formatBrl(),
    distance = formatDistance(distanceMeters),
)

internal fun ExternalStore.toCard() = ExternalStoreCardUi(
    id = id,
    name = name,
    street = street,
    neighborhood = neighborhood,
    distance = formatDistance(distanceMeters),
    whatsappLink = whatsapp?.let(::whatsappLink),
    phoneLink = phone?.let(::phoneLink),
)
