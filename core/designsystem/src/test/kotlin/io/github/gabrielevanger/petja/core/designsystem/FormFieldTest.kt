package io.github.gabrielevanger.petja.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gabrielevanger.petja.core.designsystem.component.FormField
import io.github.gabrielevanger.petja.core.designsystem.component.FormFieldType
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FormFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsLabelErrorAndTypedText() {
        var typed = ""
        composeRule.setContent {
            PetjaTheme {
                Column {
                    FormField(value = "", onValueChange = {
                        typed = it
                    }, label = "E-mail", type = FormFieldType.Email, error = "E-mail inválido")
                    FormField(value = "Ana", onValueChange = {}, label = "Nome", type = FormFieldType.Name)
                    FormField(value = "", onValueChange = {}, label = "Observação", enabled = false)
                }
            }
        }

        composeRule.onNodeWithText("E-mail inválido").assertExists()
        composeRule.onNodeWithText("E-mail").performTextInput("ana@teste.local")
        assertEquals("ana@teste.local", typed)
        composeRule.onNodeWithText("Ana").assertExists()
        composeRule.onNodeWithText("Observação").assertIsNotEnabled()
    }

    @Test
    fun passwordIsHidden() {
        composeRule.setContent {
            PetjaTheme {
                FormField(value = "segredo123", onValueChange = {}, label = "Senha", type = FormFieldType.Password)
            }
        }

        composeRule.onNodeWithText("Senha").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
    }
}
