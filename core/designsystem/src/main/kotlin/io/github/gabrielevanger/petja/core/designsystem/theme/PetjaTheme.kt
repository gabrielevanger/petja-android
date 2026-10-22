package io.github.gabrielevanger.petja.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalPetjaSpacing = staticCompositionLocalOf { PetjaSpacing() }
private val LocalPetjaSizes = staticCompositionLocalOf { PetjaSizes() }
private val LocalPetjaShapes = staticCompositionLocalOf { PetjaShapes() }
private val LocalPetjaTypography = staticCompositionLocalOf { DefaultPetjaTypography }

/**
 * Ponto único de acesso aos tokens: telas e componentes não declaram cores, medidas, formas
 * nem estilos de texto soltos. As cores são os papéis do Material (primary, surfaceVariant...)
 * mais os do PetJá (highlight, accentContainer...), com a paleta definida em PetjaColors.
 */
object PetjaTheme {
    val colors: ColorScheme
        @Composable get() = MaterialTheme.colorScheme

    val spacing: PetjaSpacing
        @Composable get() = LocalPetjaSpacing.current

    val sizes: PetjaSizes
        @Composable get() = LocalPetjaSizes.current

    val shapes: PetjaShapes
        @Composable get() = LocalPetjaShapes.current

    val typography: PetjaTypography
        @Composable get() = LocalPetjaTypography.current
}

/** Segue o modo claro ou escuro do aparelho (RNF-08). */
@Composable
fun PetjaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalPetjaSpacing provides PetjaSpacing(),
        LocalPetjaSizes provides PetjaSizes(),
        LocalPetjaShapes provides PetjaShapes(),
        LocalPetjaTypography provides DefaultPetjaTypography,
        LocalPetjaExtendedColors provides if (darkTheme) PetjaDarkExtendedColors else PetjaLightExtendedColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) PetjaDarkColors else PetjaLightColors,
            content = content,
        )
    }
}
