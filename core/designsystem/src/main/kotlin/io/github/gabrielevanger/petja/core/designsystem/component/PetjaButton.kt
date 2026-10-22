package io.github.gabrielevanger.petja.core.designsystem.component

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
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme

/** Os quatro botões do Figma (componente Button): ação principal, secundária em tom, contorno e link. */
enum class PetjaButtonStyle { Filled, Tonal, Outlined, Text }

/**
 * Botão de ação com a altura e os cantos do layout. Com [loading], mostra o progresso ao lado do
 * texto e não aceita outro toque, para não mandar o mesmo pedido duas vezes.
 */
@Composable
fun PetjaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PetjaButtonStyle = PetjaButtonStyle.Filled,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val shape = PetjaTheme.shapes.card
    val padding = PaddingValues(horizontal = PetjaTheme.spacing.l)
    val sized = modifier.heightIn(min = PetjaTheme.sizes.button)
    val clickable = enabled && !loading
    val content: @Composable RowScope.() -> Unit = {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(end = PetjaTheme.spacing.xs).size(PetjaTheme.sizes.inlineProgress),
                strokeWidth = PetjaTheme.sizes.divider * 2,
            )
        }
        Text(text = text, style = PetjaTheme.typography.label)
    }
    when (style) {
        PetjaButtonStyle.Filled ->
            Button(onClick, sized, clickable, shape = shape, contentPadding = padding, content = content)

        PetjaButtonStyle.Tonal ->
            FilledTonalButton(onClick, sized, clickable, shape = shape, contentPadding = padding, content = content)

        PetjaButtonStyle.Outlined -> OutlinedButton(
            onClick,
            sized,
            clickable,
            shape = shape,
            border = BorderStroke(PetjaTheme.sizes.divider, PetjaTheme.colors.outline),
            contentPadding = padding,
            content = content,
        )

        PetjaButtonStyle.Text ->
            TextButton(onClick, sized, clickable, shape = shape, contentPadding = padding, content = content)
    }
}
