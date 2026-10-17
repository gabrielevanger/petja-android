package io.github.gabrielevanger.xodopet.feature.storefront

import io.github.gabrielevanger.xodopet.core.domain.model.GeoPoint

/** Lugar de Boa Vista para escolher onde entregar. */
data class SamplePlace(val name: String, val point: GeoPoint)

/**
 * Pontos fixos de Boa Vista enquanto o endereço com pino no mapa e o GPS não chegam (RF-11, RF-13).
 * Cobrem os cenários do seed.sql: várias lojas, loja fechada e nenhuma parceira.
 */
internal val BoaVistaSamplePlaces = listOf(
    SamplePlace("Centro", GeoPoint(2.8200, -60.6720)),
    SamplePlace("Caçari", GeoPoint(2.8330, -60.6760)),
    SamplePlace("Pricumã", GeoPoint(2.8150, -60.7050)),
    SamplePlace("Buritis", GeoPoint(2.8230, -60.7150)),
    SamplePlace("Santa Tereza", GeoPoint(2.8040, -60.7230)),
    SamplePlace("Cidade Satélite", GeoPoint(2.8600, -60.7600)),
)
