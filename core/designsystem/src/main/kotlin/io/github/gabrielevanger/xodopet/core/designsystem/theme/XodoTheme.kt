package io.github.gabrielevanger.xodopet.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** Espaçamentos usados no app inteiro: componentes não declaram medidas soltas. */
object XodoSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

/** Segue o modo claro ou escuro do aparelho (RNF-08). */
@Composable
fun XodoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) XodoDarkColors else XodoLightColors,
        content = content,
    )
}
