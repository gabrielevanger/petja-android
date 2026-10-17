package io.github.gabrielevanger.xodopet.feature.storefront

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class StorefrontUiState(
    val places: ImmutableList<SamplePlace>,
    val selectedPlace: SamplePlace,
    val content: StorefrontContent = StorefrontContent.Loading,
)

@Immutable
sealed interface StorefrontContent {
    data object Loading : StorefrontContent

    data object Error : StorefrontContent

    /**
     * @property noPartnerDelivers nenhuma parceira entrega no endereço (RF-14).
     * @property nonPartners petshops não parceiros, quando nenhuma parceira está aberta (RF-15).
     */
    data class Loaded(
        val partners: ImmutableList<StoreCardUi> = persistentListOf(),
        val nonPartners: ImmutableList<ExternalStoreCardUi> = persistentListOf(),
    ) : StorefrontContent {
        val noPartnerDelivers: Boolean get() = partners.isEmpty()
    }
}

/** Valores já formatados; os textos em volta ("Entrega", "Pedido mínimo") ficam nos recursos. */
@Immutable
data class StoreCardUi(
    val id: String,
    val name: String,
    val neighborhood: String,
    val isOpen: Boolean,
    val estimatedMinutes: Int,
    val deliveryFee: String,
    val isFreeDelivery: Boolean,
    val minOrder: String?,
    val distance: String,
)

@Immutable
data class ExternalStoreCardUi(
    val id: String,
    val name: String,
    val address: String?,
    val distance: String,
    val whatsappLink: String?,
    val phoneLink: String?,
)
