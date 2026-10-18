package io.github.gabrielevanger.xodopet.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme

/** Aviso em destaque dentro da tela, como "a loja está fechada agora". */
@Composable
fun Notice(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = XodoTheme.colors.surfaceVariant,
        contentColor = XodoTheme.colors.onSurfaceVariant,
        shape = XodoTheme.shapes.notice,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(text = text, style = XodoTheme.typography.body, modifier = Modifier.padding(XodoTheme.spacing.m))
    }
}
