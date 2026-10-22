package io.github.gabrielevanger.petja.core.domain.storefront

import io.github.gabrielevanger.petja.core.domain.model.GeoPoint
import io.github.gabrielevanger.petja.core.domain.model.StoreSummary

/**
 * Monta a vitrine de um endereço.
 *
 * Regra: se nenhuma parceira estiver aberta para entregar agora (nenhuma no raio, ou todas
 * fechadas), também busca os petshops não parceiros, para o cliente não sair de mãos vazias.
 * A ordem das parceiras é garantida aqui, sem depender da ordem que vem do servidor.
 */
class LoadStorefront(private val repository: StorefrontRepository) {
    suspend operator fun invoke(point: GeoPoint): Storefront {
        val partners = repository.storesNear(point)
            .sortedWith(compareByDescending<StoreSummary> { it.isOpen }.thenBy { it.distanceMeters })
        val nonPartners = if (partners.any { it.isOpen }) emptyList() else repository.externalStoresNear(point)
        return Storefront(partners, nonPartners.sortedBy { it.distanceMeters })
    }
}
