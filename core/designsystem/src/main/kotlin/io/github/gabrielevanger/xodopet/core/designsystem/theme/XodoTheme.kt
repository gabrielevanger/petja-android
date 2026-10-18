package io.github.gabrielevanger.xodopet.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalXodoSpacing = staticCompositionLocalOf { XodoSpacing() }
private val LocalXodoSizes = staticCompositionLocalOf { XodoSizes() }
private val LocalXodoShapes = staticCompositionLocalOf { XodoShapes() }
private val LocalXodoTypography = staticCompositionLocalOf { DefaultXodoTypography }

/**
 * Ponto único de acesso aos tokens: telas e componentes não declaram cores, medidas, formas
 * nem estilos de texto soltos. As cores são os papéis do Material (primary, surfaceVariant...)
 * mais os do Xodó (highlight, accentContainer...), com a paleta definida em XodoColors.
 */
object XodoTheme {
    val colors: ColorScheme
        @Composable get() = MaterialTheme.colorScheme

    val spacing: XodoSpacing
        @Composable get() = LocalXodoSpacing.current

    val sizes: XodoSizes
        @Composable get() = LocalXodoSizes.current

    val shapes: XodoShapes
        @Composable get() = LocalXodoShapes.current

    val typography: XodoTypography
        @Composable get() = LocalXodoTypography.current
}

/** Segue o modo claro ou escuro do aparelho (RNF-08). */
@Composable
fun XodoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalXodoSpacing provides XodoSpacing(),
        LocalXodoSizes provides XodoSizes(),
        LocalXodoShapes provides XodoShapes(),
        LocalXodoTypography provides DefaultXodoTypography,
        LocalXodoExtendedColors provides if (darkTheme) XodoDarkExtendedColors else XodoLightExtendedColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) XodoDarkColors else XodoLightColors,
            content = content,
        )
    }
}
