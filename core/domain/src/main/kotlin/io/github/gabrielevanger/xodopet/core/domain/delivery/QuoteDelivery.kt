package io.github.gabrielevanger.xodopet.core.domain.delivery

import io.github.gabrielevanger.xodopet.core.domain.model.GeoPoint
import io.github.gabrielevanger.xodopet.core.domain.model.Money
import io.github.gabrielevanger.xodopet.core.domain.storefront.StorefrontRepository

/** Taxa e distância de uma loja para um ponto, calculadas pelo servidor (RN-07). */
data class DeliveryQuote(val fee: Money, val distanceMeters: Int, val storeIsOpen: Boolean)

class QuoteDelivery(private val storefront: StorefrontRepository) {
    /** null quando a loja não entrega no ponto (fora do raio ou desativada). */
    suspend operator fun invoke(storeId: String, point: GeoPoint): DeliveryQuote? = storefront.storesNear(point)
        .firstOrNull { it.id == storeId }
        ?.let { DeliveryQuote(it.deliveryFee, it.distanceMeters, it.isOpen) }
}
