package io.github.gabrielevanger.xodopet.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme

/** Os quatro botões do Figma (componente Button): ação principal, secundária em tom, contorno e link. */
enum class XodoButtonStyle { Filled, Tonal, Outlined, Text }

/**
 * Botão de ação com a altura e os cantos do layout. Com [loading], mostra o progresso ao lado do
 * texto e não aceita outro toque, para não mandar o mesmo pedido duas vezes.
 */
@Composable
fun XodoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: XodoButtonStyle = XodoButtonStyle.Filled,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val shape = XodoTheme.shapes.card
    val padding = PaddingValues(horizontal = XodoTheme.spacing.l)
    val sized = modifier.heightIn(min = XodoTheme.sizes.button)
    val clickable = enabled && !loading
    val content: @Composable RowScope.() -> Unit = {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(end = XodoTheme.spacing.xs).size(XodoTheme.sizes.inlineProgress),
                strokeWidth = XodoTheme.sizes.divider * 2,
            )
        }
        Text(text = text, style = XodoTheme.typography.label)
    }
    when (style) {
        XodoButtonStyle.Filled ->
            Button(onClick, sized, clickable, shape = shape, contentPadding = padding, content = content)

        XodoButtonStyle.Tonal ->
            FilledTonalButton(onClick, sized, clickable, shape = shape, contentPadding = padding, content = content)

        XodoButtonStyle.Outlined -> OutlinedButton(
            onClick,
            sized,
            clickable,
            shape = shape,
            border = BorderStroke(XodoTheme.sizes.divider, XodoTheme.colors.outline),
            contentPadding = padding,
            content = content,
        )

        XodoButtonStyle.Text ->
            TextButton(onClick, sized, clickable, shape = shape, contentPadding = padding, content = content)
    }
}
