package io.github.gabrielevanger.xodopet.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gabrielevanger.xodopet.core.designsystem.component.BottomBarItem
import io.github.gabrielevanger.xodopet.core.designsystem.component.IconTile
import io.github.gabrielevanger.xodopet.core.designsystem.component.ProductThumbnail
import io.github.gabrielevanger.xodopet.core.designsystem.component.SelectableOption
import io.github.gabrielevanger.xodopet.core.designsystem.component.SuccessIllustration
import io.github.gabrielevanger.xodopet.core.designsystem.component.XodoBottomBar
import io.github.gabrielevanger.xodopet.core.designsystem.component.XodoButton
import io.github.gabrielevanger.xodopet.core.designsystem.component.XodoButtonStyle
import io.github.gabrielevanger.xodopet.core.designsystem.component.XodoCard
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaseComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun everyButtonStyleClicks() {
        val clicks = mutableListOf<XodoButtonStyle>()
        composeRule.setContent {
            XodoTheme {
                Column {
                    XodoButtonStyle.entries.forEach { style ->
                        XodoButton(text = style.name, onClick = { clicks += style }, style = style)
                    }
                }
            }
        }

        XodoButtonStyle.entries.forEach { composeRule.onNodeWithText(it.name).performClick() }
        assertEquals(XodoButtonStyle.entries, clicks)
    }

    @Test
    fun loadingAndDisabledButtonsDoNotClick() {
        composeRule.setContent {
            XodoTheme {
                Column {
                    XodoButton(text = "Fazer pedido", onClick = {}, loading = true)
                    XodoButton(text = "Continuar", onClick = {}, enabled = false)
                    XodoButton(text = "Entrar", onClick = {})
                }
            }
        }

        composeRule.onNode(hasText("Fazer pedido") and hasClickAction()).assertIsNotEnabled()
        composeRule.onNode(hasText("Continuar") and hasClickAction()).assertIsNotEnabled()
        composeRule.onNode(hasText("Entrar") and hasClickAction()).assertIsEnabled()
    }

    @Test
    fun cardsWithAndWithoutClick() {
        var opened = 0
        composeRule.setContent {
            XodoTheme {
                Column {
                    XodoCard { Text("Resumo") }
                    XodoCard(onClick = { opened++ }) { Text("Petshop Lavrado") }
                }
            }
        }

        composeRule.onNodeWithText("Resumo").assertExists()
        composeRule.onNodeWithText("Petshop Lavrado").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun selectableOptionReportsStateAndSelects() {
        var chosen: String? = null
        composeRule.setContent {
            XodoTheme {
                Column {
                    SelectableOption(
                        title = "Dinheiro",
                        details = "Pague ao entregador",
                        selected = true,
                        onSelect = { chosen = "Dinheiro" },
                        icon = painterResource(R.drawable.ic_house),
                    )
                    SelectableOption(
                        title = "Cartão",
                        selected = false,
                        onSelect = { chosen = "Cartão" },
                        icon = painterResource(R.drawable.ic_house),
                    )
                }
            }
        }

        composeRule.onNode(hasText("Dinheiro") and hasClickAction(), useUnmergedTree = false).assertIsSelected()
        composeRule.onNode(hasText("Cartão") and hasClickAction()).assertIsNotSelected().performClick()
        assertEquals("Cartão", chosen)
    }

    @Test
    fun bottomBarMarksTheCurrentDestination() {
        var opened: String? = null
        composeRule.setContent {
            XodoTheme {
                XodoBottomBar(
                    listOf(
                        BottomBarItem("Vitrine", painterResource(R.drawable.ic_house), true) { opened = "Vitrine" },
                        BottomBarItem("Pedidos", painterResource(R.drawable.ic_receipt_text), false) {
                            opened = "Pedidos"
                        },
                        BottomBarItem("Conta", painterResource(R.drawable.ic_user_round), false) { opened = "Conta" },
                    ),
                )
            }
        }

        composeRule.onNodeWithText("Vitrine").assertIsSelected()
        composeRule.onNodeWithText("Pedidos").assertIsNotSelected().performClick()
        assertEquals("Pedidos", opened)
    }

    @Test
    fun decorativePiecesDrawWithoutText() {
        composeRule.setContent {
            XodoTheme {
                Column {
                    IconTile(painterResource(R.drawable.ic_store))
                    ProductThumbnail()
                    SuccessIllustration(painterResource(R.drawable.ic_shopping_bag))
                    Text("Pedido enviado")
                }
            }
        }

        // Ícones decorativos não aparecem para o TalkBack; o texto ao lado é que conta.
        composeRule.onNodeWithText("Pedido enviado").assertExists()
    }
}
