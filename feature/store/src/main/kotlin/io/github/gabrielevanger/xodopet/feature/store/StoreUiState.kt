package io.github.gabrielevanger.xodopet.feature.store

import androidx.compose.runtime.Immutable
import io.github.gabrielevanger.xodopet.core.domain.model.PetSpecies
import io.github.gabrielevanger.xodopet.core.ui.CartBarUi
import kotlinx.collections.immutable.ImmutableList

/**
 * Tudo o que a página da loja mostra num estado só.
 *
 * @property cartBar resumo do carrinho para a barra de baixo; null quando vazio.
 * @property replacePrompt pergunta antes de trocar o carrinho de outra loja por este (RF-41).
 */
@Immutable
data class StoreUiState(
    val content: StoreContent = StoreContent.Loading,
    val cartBar: CartBarUi? = null,
    val replacePrompt: ReplaceCartPrompt? = null,
)

@Immutable
sealed interface StoreContent {
    data object Loading : StoreContent

    data object Error : StoreContent

    /** Loja desativada, removida ou link antigo. */
    data object NotFound : StoreContent

    data class Loaded(
        val header: StoreHeaderUi,
        val sections: ImmutableList<CatalogSectionUi>,
        val selectedProduct: ProductDetailUi? = null,
    ) : StoreContent
}

@Immutable
data class ReplaceCartPrompt(val currentStoreName: String, val variantId: String)

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
