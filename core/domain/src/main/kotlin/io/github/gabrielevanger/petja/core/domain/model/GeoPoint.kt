package io.github.gabrielevanger.petja.core.domain.model

/** Ponto no mapa, em graus. */
data class GeoPoint(val latitude: Double, val longitude: Double) {
    init {
        require(latitude in -90.0..90.0) { "Latitude fora do intervalo: $latitude" }
        require(longitude in -180.0..180.0) { "Longitude fora do intervalo: $longitude" }
    }
}
