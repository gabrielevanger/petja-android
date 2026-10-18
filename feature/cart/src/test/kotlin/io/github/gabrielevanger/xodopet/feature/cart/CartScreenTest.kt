package io.github.gabrielevanger.xodopet.feature.cart

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CartScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val loaded = CartUiState.Loaded(
        storeName = "Petshop Lavrado",
        placeName = "Centro",
        lines = persistentListOf(CartLineUi("1kg", "Ração Golden", "1 kg", "R$ 30,90", "R$ 30,90", 1, true)),
        subtotal = "R$ 30,90",
        delivery = DeliveryUi.Fee("R$ 5,00", isFree = false, storeIsOpen = true),
        total = "R$ 35,90",
        minOrder = "R$ 50,00",
        missingForMinOrder = "R$ 19,10",
    )

    private fun show(
        state: CartUiState,
        onIncrease: (String, Int) -> Unit = { _, _ ->
        },
        onDecrease: (String, Int) -> Unit = { _, _ -> },
    ) {
        composeRule.setContent {
            XodoTheme { CartScreen(state, onBack = {}, onIncrease = onIncrease, onDecrease = onDecrease, onClear = {}) }
        }
    }

    @Test
    fun showsSummaryAndMinimumOrder() {
        show(loaded)

        composeRule.onNodeWithText("Petshop Lavrado").assertExists()
        composeRule.onNodeWithText("1 item no carrinho · Entregar em Centro").assertExists()
        composeRule.onNodeWithText("R$ 5,00").assertExists()
        composeRule.onNodeWithText("R$ 35,90").assertExists()
        composeRule.onNodeWithText("Faltam R$ 19,10 para o pedido mínimo de R$ 50,00.").assertExists()
        composeRule.onNodeWithText("Entrar para finalizar").assertIsNotEnabled()
    }

    @Test
    fun stepperButtonsDescribeTheItem() {
        var increased: Pair<String, Int>? = null
        var decreased: Pair<String, Int>? = null
        show(loaded, onIncrease = { id, q -> increased = id to q }, onDecrease = { id, q -> decreased = id to q })

        // O resumo fixo embaixo ocupa parte da tela; a lista rola até o item antes do toque.
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasContentDescription("Adicionar uma unidade de Ração Golden"))
        composeRule.onNodeWithContentDescription("Adicionar uma unidade de Ração Golden").performClick()
        composeRule.onNodeWithContentDescription("Remover Ração Golden do carrinho").performClick()

        assertEquals("1kg" to 1, increased)
        assertEquals("1kg" to 1, decreased)
    }

    @Test
    fun outOfAreaExplainsWhatToDo() {
        show(loaded.copy(delivery = DeliveryUi.OutOfArea, total = null))

        composeRule.onNodeWithText("A loja não entrega em Centro", substring = true).assertExists()
    }

    @Test
    fun emptyCart() {
        show(CartUiState.Empty)

        composeRule.onNodeWithText("Seu carrinho está vazio", substring = true).assertExists()
    }

    @Test
    fun checkoutButtonSignsInOrContinues() {
        var signIn = 0
        var checkout = 0
        val ready = loaded.copy(missingForMinOrder = null)
        composeRule.setContent {
            XodoTheme {
                CartScreen(ready, {
                }, { _, _ -> }, { _, _ -> }, {}, onCheckout = { checkout++ }, onSignIn = { signIn++ })
            }
        }

        composeRule.onNodeWithText("Entrar para finalizar").performClick()
        assertEquals(1, signIn)
        assertEquals(0, checkout)
    }

    @Test
    fun signedInGoesToCheckout() {
        var checkout = 0
        composeRule.setContent {
            XodoTheme {
                CartScreen(loaded.copy(missingForMinOrder = null, signedIn = true), {
                }, { _, _ -> }, { _, _ -> }, {}, onCheckout = { checkout++ })
            }
        }

        composeRule.onNodeWithText("Continuar").performClick()
        assertEquals(1, checkout)
    }
}
