package io.github.gabrielevanger.petja.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Paleta do lavrado de Roraima (REQUISITOS.md, seção 13.2):
// dourado do capim seco (primária), verde do buriti (secundária) e azul do céu de Boa Vista (terciária),
// sobre fundos em tons quentes. No escuro, os mesmos tons ficam mais claros para manter o contraste.

internal val PetjaLightColors = lightColorScheme(
    primary = Color(0xFF7F5300),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDDB3),
    onPrimaryContainer = Color(0xFF291800),
    secondary = Color(0xFF3B6B2F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFBDF2A8),
    onSecondaryContainer = Color(0xFF042100),
    tertiary = Color(0xFF22638A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCAE6FF),
    onTertiaryContainer = Color(0xFF001E30),
    background = Color(0xFFFFF8F2),
    onBackground = Color(0xFF211A13),
    surface = Color(0xFFFFF8F2),
    onSurface = Color(0xFF211A13),
    surfaceVariant = Color(0xFFF1E0CF),
    onSurfaceVariant = Color(0xFF50453A),
    surfaceContainer = Color(0xFFF8ECE0),
    surfaceContainerHigh = Color(0xFFF2E6DA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    outline = Color(0xFF837468),
    outlineVariant = Color(0xFFD5C4B4),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

internal val PetjaDarkColors = darkColorScheme(
    primary = Color(0xFFF8BB5C),
    onPrimary = Color(0xFF452B00),
    primaryContainer = Color(0xFF613E00),
    onPrimaryContainer = Color(0xFFFFDDB3),
    secondary = Color(0xFFA2D58E),
    onSecondary = Color(0xFF0D3905),
    secondaryContainer = Color(0xFF235119),
    onSecondaryContainer = Color(0xFFBDF2A8),
    tertiary = Color(0xFF93CCF9),
    onTertiary = Color(0xFF00344F),
    tertiaryContainer = Color(0xFF004B70),
    onTertiaryContainer = Color(0xFFCAE6FF),
    background = Color(0xFF18120C),
    onBackground = Color(0xFFEDE0D4),
    surface = Color(0xFF18120C),
    onSurface = Color(0xFFEDE0D4),
    surfaceVariant = Color(0xFF50453A),
    onSurfaceVariant = Color(0xFFD5C4B4),
    surfaceContainer = Color(0xFF251E17),
    surfaceContainerHigh = Color(0xFF302821),
    surfaceContainerLowest = Color(0xFF120D08),
    outline = Color(0xFF9E8E80),
    outlineVariant = Color(0xFF50453A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

/**
 * Papéis que o Material não tem: o verde da mata para destaques (perfil, status do pedido,
 * opção escolhida) e o pêssego para avatar e foto que ainda não existe. Mesmos nomes das
 * variáveis do Figma (color/highlight...).
 */
@Immutable
data class PetjaExtendedColors(
    val highlight: Color,
    val onHighlight: Color,
    val highlightContainer: Color,
    val onHighlightContainer: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
)

internal val PetjaLightExtendedColors = PetjaExtendedColors(
    highlight = Color(0xFF254D3E),
    onHighlight = Color(0xFFFFFFFF),
    highlightContainer = Color(0xFFE5EDDF),
    onHighlightContainer = Color(0xFF0A2119),
    accentContainer = Color(0xFFF3D6BC),
    onAccentContainer = Color(0xFF3B2511),
)

internal val PetjaDarkExtendedColors = PetjaExtendedColors(
    highlight = Color(0xFFA3CFB9),
    onHighlight = Color(0xFF13382A),
    highlightContainer = Color(0xFF13382A),
    onHighlightContainer = Color(0xFFC9E9D6),
    accentContainer = Color(0xFF5A4129),
    onAccentContainer = Color(0xFFFBE5D1),
)

internal val LocalPetjaExtendedColors = staticCompositionLocalOf { PetjaLightExtendedColors }

// Ficam como propriedades de PetjaTheme.colors, junto dos papéis do Material.
val ColorScheme.highlight: Color
    @Composable get() = LocalPetjaExtendedColors.current.highlight

val ColorScheme.onHighlight: Color
    @Composable get() = LocalPetjaExtendedColors.current.onHighlight

val ColorScheme.highlightContainer: Color
    @Composable get() = LocalPetjaExtendedColors.current.highlightContainer

val ColorScheme.onHighlightContainer: Color
    @Composable get() = LocalPetjaExtendedColors.current.onHighlightContainer

val ColorScheme.accentContainer: Color
    @Composable get() = LocalPetjaExtendedColors.current.accentContainer

val ColorScheme.onAccentContainer: Color
    @Composable get() = LocalPetjaExtendedColors.current.onAccentContainer
