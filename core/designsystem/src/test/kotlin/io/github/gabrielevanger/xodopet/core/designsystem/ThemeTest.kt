package io.github.gabrielevanger.xodopet.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun lightAndDarkUseDifferentBackgrounds() {
        var light = Color.Unspecified
        var dark = Color.Unspecified
        composeRule.setContent {
            XodoTheme(darkTheme = false) { light = XodoTheme.colors.background }
            XodoTheme(darkTheme = true) { dark = XodoTheme.colors.background }
        }

        assertNotEquals(light, dark)
    }

    @Test
    fun tokensComeFromTheTheme() {
        var spacingM = 0.dp
        var touchTarget = 0.dp
        var priceUsesTabularDigits = false
        composeRule.setContent {
            XodoTheme {
                spacingM = XodoTheme.spacing.m
                touchTarget = XodoTheme.sizes.minTouchTarget
                priceUsesTabularDigits = XodoTheme.typography.price.fontFeatureSettings == "tnum"
                XodoTheme.shapes.card
            }
        }

        assertEquals(16.dp, spacingM)
        assertEquals(48.dp, touchTarget)
        assertEquals(true, priceUsesTabularDigits)
    }
}
