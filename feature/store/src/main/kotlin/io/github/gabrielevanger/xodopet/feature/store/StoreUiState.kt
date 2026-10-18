package io.github.gabrielevanger.xodopet.feature.store

import androidx.compose.runtime.Immutable
import io.github.gabrielevanger.xodopet.core.domain.model.PetSpecies
import kotlinx.collections.immutable.ImmutableList

@Immutable
sealed interface StoreUiState {
    data object Loading : StoreUiState

    data object Error : StoreUiState

    /** Loja desativada, removida ou link antigo. */
    data object NotFound : StoreUiState

    data class Loaded(
        val header: StoreHeaderUi,
        val sections: ImmutableList<CatalogSectionUi>,
        val selectedProduct: ProductDetailUi? = null,
    ) : StoreUiState
}

@Immutable
data class StoreHeaderUi(
    val name: String,
    val neighborhood: String,
    val description: String?,
    val isOpen: Boolean,
    val estimatedMinutes: Int,
    val minOrder: String?,
)

@Immutable
data class CatalogSectionUi(val categoryId: Int, val title: String, val products: ImmutableList<ProductRowUi>)

/**
 * @property price menor preço disponível; null quando nada está disponível.
 * @property fromPrice true quando há variantes com preços diferentes, para mostrar "a partir de".
 */
@Immutable
data class ProductRowUi(
    val id: String,
    val name: String,
    val brand: String?,
    val price: String?,
    val fromPrice: Boolean,
    val isAvailable: Boolean,
)

@Immutable
data class ProductDetailUi(
    val id: String,
    val name: String,
    val brand: String?,
    val description: String?,
    val species: ImmutableList<PetSpecies>,
    val variants: ImmutableList<VariantUi>,
)

@Immutable
data class VariantUi(val id: String, val label: String, val price: String, val isAvailable: Boolean)
