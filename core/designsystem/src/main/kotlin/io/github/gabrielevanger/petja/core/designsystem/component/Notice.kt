package io.github.gabrielevanger.petja.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme

/** Aviso em destaque dentro da tela, como "a loja está fechada agora". */
@Composable
fun Notice(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = PetjaTheme.colors.surfaceVariant,
        contentColor = PetjaTheme.colors.onSurfaceVariant,
        shape = PetjaTheme.shapes.notice,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(text = text, style = PetjaTheme.typography.body, modifier = Modifier.padding(PetjaTheme.spacing.m))
    }
}
