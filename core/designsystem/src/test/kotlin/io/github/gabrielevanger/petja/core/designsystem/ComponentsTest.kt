package io.github.gabrielevanger.petja.core.designsystem

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gabrielevanger.petja.core.designsystem.component.BadgeTone
import io.github.gabrielevanger.petja.core.designsystem.component.ErrorState
import io.github.gabrielevanger.petja.core.designsystem.component.LoadingState
import io.github.gabrielevanger.petja.core.designsystem.component.MessageState
import io.github.gabrielevanger.petja.core.designsystem.component.Notice
import io.github.gabrielevanger.petja.core.designsystem.component.QuantityStepper
import io.github.gabrielevanger.petja.core.designsystem.component.SectionTitle
import io.github.gabrielevanger.petja.core.designsystem.component.StatusBadge
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun badgesShowTheirText() {
        composeRule.setContent {
            PetjaTheme {
                StatusBadge("Aberta", BadgeTone.Positive)
                StatusBadge("Fechada", BadgeTone.Neutral)
            }
        }

        composeRule.onNodeWithText("Aberta").assertExists()
        composeRule.onNodeWithText("Fechada").assertExists()
    }

    @Test
    fun sectionTitleIsAHeadingForTalkBack() {
        composeRule.setContent { PetjaTheme { SectionTitle("Ração") } }

        composeRule.onNodeWithText("Ração").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun noticeAndScreenStates() {
        var retries = 0
        composeRule.setContent {
            PetjaTheme {
                Notice("Loja fechada")
                LoadingState("Carregando")
                MessageState("Nada aqui")
                ErrorState("Falhou", actionLabel = "Tentar de novo", onAction = { retries++ })
            }
        }

        composeRule.onNodeWithText("Loja fechada").assertExists()
        composeRule.onNodeWithText("Carregando").assertExists()
        composeRule.onNodeWithText("Nada aqui").assertExists()
        composeRule.onNodeWithText("Tentar de novo").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun stepperChangesQuantityAndRespectsTheLimit() {
        var increased = 0
        var decreased = 0
        composeRule.setContent {
            PetjaTheme {
                QuantityStepper(
                    quantity = 2,
                    canIncrease = false,
                    onDecrease = { decreased++ },
                    onIncrease = { increased++ },
                    decreaseDescription = "Tirar um",
                    increaseDescription = "Pôr um",
                )
            }
        }

        composeRule.onNodeWithText("2").assertExists()
        composeRule.onNodeWithContentDescription("Pôr um").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Tirar um").assertIsEnabled().performClick()
        assertEquals(1, decreased)
        assertEquals(0, increased)
    }

    @Test
    fun stepperWithOneUnitStillDecreases() {
        var decreased = 0
        var increased = 0
        composeRule.setContent {
            PetjaTheme {
                QuantityStepper(
                    quantity = 1,
                    canIncrease = true,
                    onDecrease = { decreased++ },
                    onIncrease = { increased++ },
                    decreaseDescription = "Remover",
                    increaseDescription = "Pôr um",
                )
            }
        }

        composeRule.onNodeWithContentDescription("Remover").performClick()
        composeRule.onNodeWithContentDescription("Pôr um").performClick()
        assertEquals(1, decreased)
        assertEquals(1, increased)
    }
}
