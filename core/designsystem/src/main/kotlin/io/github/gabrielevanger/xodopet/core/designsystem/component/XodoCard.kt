package io.github.gabrielevanger.xodopet.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme

/**
 * Cartão claro sobre o fundo da tela, sem sombra, como os do layout. Com [onClick], o cartão
 * inteiro é o alvo de toque (abre a loja, o produto, o pedido).
 */
@Composable
fun XodoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = XodoTheme.shapes.card,
    color: Color = XodoTheme.colors.surfaceContainerLowest,
    contentPadding: Dp = XodoTheme.spacing.m,
    content: @Composable ColumnScope.() -> Unit,
) {
    val inner: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
    if (onClick == null) {
        Surface(shape = shape, color = color, modifier = modifier.fillMaxWidth(), content = inner)
    } else {
        Surface(onClick = onClick, shape = shape, color = color, modifier = modifier.fillMaxWidth(), content = inner)
    }
}
