package io.github.gabrielevanger.petja.core.domain.model

/** Endereço salvo do cliente. O ponto no mapa decide quais lojas entregam e a taxa (RN-02, RN-07). */
data class Address(
    val id: String,
    val label: String?,
    val street: String,
    val number: String,
    val complement: String?,
    val neighborhood: String,
    val reference: String?,
    val location: GeoPoint,
    val isDefault: Boolean,
)

/** Endereço digitado no checkout, antes de ser salvo. */
data class NewAddress(
    val street: String,
    val number: String,
    val complement: String?,
    val neighborhood: String,
    val reference: String?,
    val location: GeoPoint,
)
