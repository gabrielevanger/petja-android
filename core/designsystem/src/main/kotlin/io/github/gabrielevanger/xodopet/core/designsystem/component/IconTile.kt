package io.github.gabrielevanger.xodopet.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme
import io.github.gabrielevanger.xodopet.core.designsystem.theme.highlight
import io.github.gabrielevanger.xodopet.core.designsystem.theme.highlightContainer

/** Ícone num quadrado verde claro, ao lado de um título (a loja no topo do carrinho). Só decora. */
@Composable
fun IconTile(icon: Painter, modifier: Modifier = Modifier) {
    Surface(color = XodoTheme.colors.highlightContainer, shape = XodoTheme.shapes.card, modifier = modifier) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = XodoTheme.colors.highlight,
            modifier = Modifier.padding(XodoTheme.spacing.s).size(XodoTheme.sizes.icon),
        )
    }
}
