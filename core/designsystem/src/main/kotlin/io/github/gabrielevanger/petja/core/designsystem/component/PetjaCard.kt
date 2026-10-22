package io.github.gabrielevanger.petja.core.designsystem.component

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
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme

/**
 * Cartão claro sobre o fundo da tela, sem sombra, como os do layout. Com [onClick], o cartão
 * inteiro é o alvo de toque (abre a loja, o produto, o pedido).
 */
@Composable
fun PetjaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = PetjaTheme.shapes.card,
    color: Color = PetjaTheme.colors.surfaceContainerLowest,
    contentPadding: Dp = PetjaTheme.spacing.m,
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
