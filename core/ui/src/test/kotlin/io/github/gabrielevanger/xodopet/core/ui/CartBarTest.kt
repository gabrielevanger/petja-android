package io.github.gabrielevanger.xodopet.core.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme
import io.github.gabrielevanger.xodopet.core.domain.model.Cart
import io.github.gabrielevanger.xodopet.core.domain.model.CartLine
import io.github.gabrielevanger.xodopet.core.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CartBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsCountSubtotalAndStore() {
        var clicks = 0
        composeRule.setContent {
            XodoTheme { CartBar(CartBarUi("Petshop Lavrado", 3, "R$ 92,70"), onClick = { clicks++ }) }
        }

        composeRule.onNodeWithText("3 itens · R$ 92,70").assertExists()
        composeRule.onNodeWithText("Petshop Lavrado").assertExists()
        composeRule.onNodeWithText("Ver carrinho").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun singularItem() {
        composeRule.setContent { XodoTheme { CartBar(CartBarUi("Casa do Bicho", 1, "R$ 8,90"), onClick = {}) } }

        composeRule.onNodeWithText("1 item · R$ 8,90").assertExists()
    }

    @Test
    fun separatorSkipsEmptyParts() {
        composeRule.setContent { XodoTheme { Text(dotSeparated("Centro", null, "", "440 m")) } }

        composeRule.onNodeWithText("Centro · 440 m").assertExists()
    }

    @Test
    fun cartBecomesBarSummary() {
        val line = CartLine("v", "p", "Ração", "1 kg", Money(3090), quantity = 3, maxQuantity = 5)

        assertEquals(
            CartBarUi("Petshop Lavrado", 3, "R$ 92,70"),
            Cart("s", "Petshop Lavrado", Money(0), listOf(line)).toCartBar(),
        )
        assertNull((null as Cart?).toCartBar())
    }
}
