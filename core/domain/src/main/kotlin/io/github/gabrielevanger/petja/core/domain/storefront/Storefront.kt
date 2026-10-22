package io.github.gabrielevanger.petja.core.domain.storefront

import io.github.gabrielevanger.petja.core.domain.model.ExternalStore
import io.github.gabrielevanger.petja.core.domain.model.StoreSummary

/**
 * Vitrine para um endereço.
 *
 * @property partners lojas parceiras que entregam no endereço, abertas primeiro e depois por distância.
 * @property nonPartners petshops não parceiros, preenchido só quando nenhuma parceira está aberta
 *   para entregar agora (RF-14, RF-15).
 */
data class Storefront(val partners: List<StoreSummary>, val nonPartners: List<ExternalStore>) {
    val hasOpenPartner: Boolean get() = partners.any { it.isOpen }
}
