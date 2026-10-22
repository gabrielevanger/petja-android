package io.github.gabrielevanger.petja.core.designsystem

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
import io.github.gabrielevanger.petja.core.designsystem.component.Avatar
import io.github.gabrielevanger.petja.core.designsystem.component.BottomBarItem
import io.github.gabrielevanger.petja.core.designsystem.component.IconTile
import io.github.gabrielevanger.petja.core.designsystem.component.PetjaBottomBar
import io.github.gabrielevanger.petja.core.designsystem.component.PetjaButton
import io.github.gabrielevanger.petja.core.designsystem.component.PetjaButtonStyle
import io.github.gabrielevanger.petja.core.designsystem.component.PetjaCard
import io.github.gabrielevanger.petja.core.designsystem.component.ProductThumbnail
import io.github.gabrielevanger.petja.core.designsystem.component.SelectableOption
import io.github.gabrielevanger.petja.core.designsystem.component.SuccessIllustration
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme
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
        val clicks = mutableListOf<PetjaButtonStyle>()
        composeRule.setContent {
            PetjaTheme {
                Column {
                    PetjaButtonStyle.entries.forEach { style ->
                        PetjaButton(text = style.name, onClick = { clicks += style }, style = style)
                    }
                }
            }
        }

        PetjaButtonStyle.entries.forEach { composeRule.onNodeWithText(it.name).performClick() }
        assertEquals(PetjaButtonStyle.entries, clicks)
    }

    @Test
    fun loadingAndDisabledButtonsDoNotClick() {
        composeRule.setContent {
            PetjaTheme {
                Column {
                    PetjaButton(text = "Fazer pedido", onClick = {}, loading = true)
                    PetjaButton(text = "Continuar", onClick = {}, enabled = false)
                    PetjaButton(text = "Entrar", onClick = {})
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
            PetjaTheme {
                Column {
                    PetjaCard { Text("Resumo") }
                    PetjaCard(onClick = { opened++ }) { Text("Petshop Lavrado") }
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
            PetjaTheme {
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
            PetjaTheme {
                PetjaBottomBar(
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
            PetjaTheme {
                Column {
                    IconTile(painterResource(R.drawable.ic_store))
                    ProductThumbnail()
                    SuccessIllustration(painterResource(R.drawable.ic_shopping_bag))
                    Avatar()
                    Text("Pedido enviado")
                }
            }
        }

        // Ícones decorativos não aparecem para o TalkBack; o texto ao lado é que conta.
        composeRule.onNodeWithText("Pedido enviado").assertExists()
    }
}
