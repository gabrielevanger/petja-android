package io.github.gabrielevanger.xodopet.feature.storefront

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StorefrontScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val places = BoaVistaSamplePlaces.toImmutableList()

    private fun show(
        content: StorefrontContent,
        onSelectPlace: (SamplePlace) -> Unit = {},
        onRetry: () -> Unit = {},
        onStoreClick: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            XodoTheme {
                StorefrontScreen(
                    state = StorefrontUiState(places, places.first(), content),
                    onSelectPlace = onSelectPlace,
                    onRetry = onRetry,
                    onStoreClick = onStoreClick,
                )
            }
        }
    }

    @Test
    fun showsPartnerStoresWithStatusAndPrices() {
        show(
            StorefrontContent.Loaded(
                partners = persistentListOf(
                    StoreCardUi("1", "Petshop Lavrado", "Centro", true, 40, "R$ 5,00", false, "R$ 30,00", "440 m"),
                    StoreCardUi("2", "Bicho Feliz", "Santa Tereza", false, 35, "R$ 0,00", true, null, "3,6 km"),
                ),
            ),
        )

        composeRule.onNodeWithText("Petshops que entregam aqui").assertExists()
        composeRule.onNodeWithText("Petshop Lavrado").assertExists()
        composeRule.onNodeWithText("Aberta").assertExists()
        composeRule.onNodeWithText("Fechada").assertExists()
        composeRule.onNodeWithText("Entrega R$ 5,00").assertExists()
        composeRule.onNodeWithText("Entrega grátis").assertExists()
        composeRule.onNodeWithText("Pedido mínimo R$ 30,00").assertExists()
        composeRule.onNodeWithText("Centro").assertIsSelected()
    }

    @Test
    fun withoutPartnersShowsNonPartnerContacts() {
        show(
            StorefrontContent.Loaded(
                nonPartners = persistentListOf(
                    ExternalStoreCardUi("e1", "Agropet Satélite", null, "310 m", "https://wa.me/1", "tel:1"),
                ),
            ),
        )

        composeRule.onNodeWithText("Nenhuma loja parceira entrega aqui ainda").assertExists()
        composeRule.onNodeWithText("Outros petshops perto de você").assertExists()
        composeRule.onNodeWithText("Agropet Satélite").assertExists()
        composeRule.onNodeWithText("WhatsApp").assertExists()
        composeRule.onNodeWithText("Ligar").assertExists()
    }

    @Test
    fun errorOffersRetry() {
        var retries = 0
        show(StorefrontContent.Error, onRetry = { retries++ })

        composeRule.onNodeWithText("Tentar de novo").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun tappingAPlaceSelectsIt() {
        var selected: SamplePlace? = null
        show(StorefrontContent.Loading, onSelectPlace = { selected = it })

        composeRule.onNodeWithText("Caçari").performClick()

        assertEquals("Caçari", selected?.name)
    }

    @Test
    fun tappingAStoreOpensIt() {
        var opened: String? = null
        show(
            StorefrontContent.Loaded(
                partners = persistentListOf(
                    StoreCardUi("loja-1", "Petshop Lavrado", "Centro", true, 40, "R$ 5,00", false, null, "440 m"),
                ),
            ),
            onStoreClick = { opened = it },
        )

        composeRule.onNodeWithText("Petshop Lavrado").performClick()

        assertEquals("loja-1", opened)
    }
}
