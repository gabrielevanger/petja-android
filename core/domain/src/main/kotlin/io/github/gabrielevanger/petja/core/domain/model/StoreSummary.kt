package io.github.gabrielevanger.petja.core.domain.model

/** Loja parceira como aparece na vitrine, já com distância e taxa calculadas para o endereço do cliente. */
data class StoreSummary(
    val id: String,
    val name: String,
    val slug: String,
    val description: String?,
    val logoUrl: String?,
    val neighborhood: String,
    val whatsapp: String?,
    val isOpen: Boolean,
    val minOrder: Money,
    val estimatedMinutes: Int,
    val distanceMeters: Int,
    val deliveryFee: Money,
)
