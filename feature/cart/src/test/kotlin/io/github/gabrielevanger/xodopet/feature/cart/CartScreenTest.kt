package io.github.gabrielevanger.xodopet.feature.cart

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

        composeRule.onNodeWithText("Pedido na Petshop Lavrado").assertExists()
        composeRule.onNodeWithText("Entregar em Centro").assertExists()
        composeRule.onNodeWithText("R$ 5,00").assertExists()
        composeRule.onNodeWithText("R$ 35,90").assertExists()
        composeRule.onNodeWithText("Faltam R$ 19,10 para o pedido mínimo de R$ 50,00.").assertExists()
        composeRule.onNodeWithText("Finalizar pedido").assertIsNotEnabled()
    }

    @Test
    fun stepperButtonsDescribeTheItem() {
        var increased: Pair<String, Int>? = null
        var decreased: Pair<String, Int>? = null
        show(loaded, onIncrease = { id, q -> increased = id to q }, onDecrease = { id, q -> decreased = id to q })

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
}
