package io.github.gabrielevanger.xodopet.core.domain.model

/** Onde entregar: por enquanto um dos pontos de teste da vitrine; depois, o endereço com pino no mapa (RF-11). */
data class DeliveryPlace(val name: String, val point: GeoPoint)
