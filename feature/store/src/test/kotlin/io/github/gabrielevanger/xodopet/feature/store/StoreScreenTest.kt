package io.github.gabrielevanger.xodopet.feature.store

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme
import io.github.gabrielevanger.xodopet.core.domain.model.PetSpecies
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoreScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val header = StoreHeaderUi("Petshop Lavrado", "Centro", "Ração e petiscos.", true, 40, "R$ 30,00")
    private val sections = persistentListOf(
        CatalogSectionUi(
            1,
            "Ração",
            persistentListOf(
                ProductRowUi("golden", "Ração Golden Adultos", "Golden", "R$ 30,90", true, true),
                ProductRowUi("pedigree", "Ração Pedigree Adultos", "Pedigree", null, false, false),
            ),
        ),
        CatalogSectionUi(
            2,
            "Petiscos",
            persistentListOf(ProductRowUi("bifinho", "Bifinho Keldog", null, "R$ 8,90", false, true)),
        ),
    )

    private fun show(
        state: StoreUiState,
        onBack: () -> Unit = {},
        onRetry: () -> Unit = {},
        onProductClick: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            XodoTheme {
                StoreScreen(state, onBack, onRetry, onProductClick, onProductDismiss = {})
            }
        }
    }

    @Test
    fun showsHeaderSectionsAndPrices() {
        show(StoreUiState.Loaded(header, sections))

        composeRule.onNodeWithText("Petshop Lavrado").assertExists()
        composeRule.onNodeWithText("Entrega em cerca de 40 min · Pedido mínimo R$ 30,00").assertExists()
        composeRule.onNodeWithText("a partir de R$ 30,90").assertExists()
        composeRule.onNodeWithText("Indisponível").assertExists()
    }

    @Test
    fun categoryChipScrollsToItsSection() {
        show(StoreUiState.Loaded(header, sections))

        // O chip vem antes do título da seção na árvore; o título pode nem estar composto ainda.
        composeRule.onAllNodesWithText("Petiscos")[0].performClick()

        composeRule.onNodeWithText("R$ 8,90").assertIsDisplayed()
    }

    @Test
    fun closedStoreExplainsThatOrdersAreNotPossible() {
        show(StoreUiState.Loaded(header.copy(isOpen = false), sections))

        composeRule.onNodeWithText("Fechada").assertExists()
        composeRule.onNodeWithText("A loja está fechada agora", substring = true).assertExists()
    }

    @Test
    fun tappingAProductOpensIt() {
        var opened: String? = null
        show(StoreUiState.Loaded(header, sections), onProductClick = { opened = it })

        composeRule.onNodeWithText("Ração Golden Adultos").performClick()

        assertEquals("golden", opened)
    }

    @Test
    fun productSheetListsOptions() {
        val detail = ProductDetailUi(
            "golden",
            "Ração Golden Adultos",
            "Golden",
            "Ração premium.",
            persistentListOf(PetSpecies.DOG),
            persistentListOf(VariantUi("1", "1 kg", "R$ 30,90", true), VariantUi("15", "15 kg", "R$ 219,90", false)),
        )
        show(StoreUiState.Loaded(header, sections, selectedProduct = detail))

        composeRule.onNodeWithText("Golden · Cães").assertExists()
        composeRule.onNodeWithText("1 kg").assertExists()
        composeRule.onNodeWithText("R$ 30,90").assertExists()
    }

    @Test
    fun backAndRetry() {
        var back = 0
        var retries = 0
        show(StoreUiState.Error, onBack = { back++ }, onRetry = { retries++ })

        composeRule.onNodeWithContentDescription("Voltar").performClick()
        composeRule.onNodeWithText("Tentar de novo").performClick()

        assertEquals(1, back)
        assertEquals(1, retries)
    }
}
