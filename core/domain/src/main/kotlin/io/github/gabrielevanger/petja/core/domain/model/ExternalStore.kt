package io.github.gabrielevanger.petja.core.domain.model

/** Petshop que não é parceiro: o app só mostra o contato, sem pedido pelo app (RF-15). */
data class ExternalStore(
    val id: String,
    val name: String,
    val phone: String?,
    val whatsapp: String?,
    val street: String?,
    val neighborhood: String?,
    val location: GeoPoint,
    val distanceMeters: Int,
)
