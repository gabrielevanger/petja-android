package io.github.gabrielevanger.petja.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme
import io.github.gabrielevanger.petja.core.designsystem.theme.highlight
import io.github.gabrielevanger.petja.core.designsystem.theme.highlightContainer

/**
 * Ícone num quadrado de cantos arredondados, ao lado de um título: a loja no topo do carrinho, a
 * situação de um pedido. Verde claro por padrão. Só decora.
 */
@Composable
fun IconTile(
    icon: Painter,
    modifier: Modifier = Modifier,
    containerColor: Color = PetjaTheme.colors.highlightContainer,
    contentColor: Color = PetjaTheme.colors.highlight,
) {
    Surface(color = containerColor, shape = PetjaTheme.shapes.card, modifier = modifier) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.padding(PetjaTheme.spacing.s).size(PetjaTheme.sizes.icon),
        )
    }
}
