package io.github.gabrielevanger.xodopet.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Paleta do lavrado de Roraima (REQUISITOS.md, seção 13.2):
// dourado do capim seco (primária), verde do buriti (secundária) e azul do céu de Boa Vista (terciária),
// sobre fundos em tons quentes. No escuro, os mesmos tons ficam mais claros para manter o contraste.

internal val XodoLightColors = lightColorScheme(
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
    outline = Color(0xFF837468),
    outlineVariant = Color(0xFFD5C4B4),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

internal val XodoDarkColors = darkColorScheme(
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
    outline = Color(0xFF9E8E80),
    outlineVariant = Color(0xFF50453A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)
