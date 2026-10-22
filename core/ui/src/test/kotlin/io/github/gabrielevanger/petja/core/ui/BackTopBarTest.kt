package io.github.gabrielevanger.petja.core.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackTopBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun backAndTitle() {
        var back = 0
        composeRule.setContent { PetjaTheme { BackTopBar("Sua conta", onBack = { back++ }) } }

        composeRule.onNodeWithText("Sua conta").assertExists()
        composeRule.onNodeWithContentDescription("Voltar").performClick()
        assertEquals(1, back)
    }
}
