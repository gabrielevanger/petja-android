package io.github.gabrielevanger.xodopet.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme
import io.github.gabrielevanger.xodopet.core.designsystem.theme.accentContainer
import io.github.gabrielevanger.xodopet.core.designsystem.theme.highlight
import io.github.gabrielevanger.xodopet.core.designsystem.theme.highlightContainer
import io.github.gabrielevanger.xodopet.core.designsystem.theme.onAccentContainer
import io.github.gabrielevanger.xodopet.core.designsystem.theme.onHighlight
import io.github.gabrielevanger.xodopet.core.designsystem.theme.onHighlightContainer
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

    @Test
    fun highlightColorsFollowTheMode() {
        val light = mutableListOf<Color>()
        val dark = mutableListOf<Color>()
        composeRule.setContent {
            listOf(false to light, true to dark).forEach { (darkTheme, into) ->
                XodoTheme(darkTheme = darkTheme) {
                    val colors = XodoTheme.colors
                    into += listOf(
                        colors.highlight,
                        colors.onHighlight,
                        colors.highlightContainer,
                        colors.onHighlightContainer,
                        colors.accentContainer,
                        colors.onAccentContainer,
                        colors.surfaceContainerLowest,
                    )
                }
            }
        }

        assertEquals(Color(0xFF254D3E), light.first())
        assertEquals(Color.White, light.last())
        light.zip(dark).forEach { (l, d) -> assertNotEquals(l, d) }
    }

    @Test
    fun headlinesUseManrope() {
        var sameFamily = false
        var bodyIsDefault = false
        composeRule.setContent {
            XodoTheme {
                val type = XodoTheme.typography
                sameFamily = listOf(type.headline, type.storeTitle, type.productPrice, type.logo, type.tagline)
                    .all { it.fontFamily == type.topBarTitle.fontFamily && it.fontFamily != null }
                bodyIsDefault = type.body.fontFamily == null
            }
        }

        assertEquals(true, sameFamily)
        assertEquals(true, bodyIsDefault)
    }
}
