package io.github.gabrielevanger.petja.core.domain.model

/** Dados da loja para a página dela; distância e taxa dependem do endereço e ficam na vitrine e no checkout. */
data class StoreDetails(
    val id: String,
    val name: String,
    val description: String?,
    val neighborhood: String,
    val whatsapp: String?,
    val isOpen: Boolean,
    val minOrder: Money,
    val estimatedMinutes: Int,
)
