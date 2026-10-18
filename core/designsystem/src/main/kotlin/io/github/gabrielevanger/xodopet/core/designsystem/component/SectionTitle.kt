package io.github.gabrielevanger.xodopet.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme

/** Título de seção, marcado como cabeçalho para o TalkBack navegar por seções (RNF-07). */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = XodoTheme.typography.sectionTitle,
        color = XodoTheme.colors.onBackground,
        modifier = modifier.semantics { heading() },
    )
}
