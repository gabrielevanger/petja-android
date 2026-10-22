package io.github.gabrielevanger.petja.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme
import io.github.gabrielevanger.petja.core.designsystem.theme.highlight
import io.github.gabrielevanger.petja.core.designsystem.theme.highlightContainer

/**
 * Uma opção de escolha única (endereço, forma de pagamento): a linha inteira é tocável e a
 * escolhida ganha o fundo verde. Quem chama agrupa as opções com `Modifier.selectableGroup()`.
 */
@Composable
fun SelectableOption(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
    icon: Painter,
    modifier: Modifier = Modifier,
    details: String? = null,
) {
    val colors = PetjaTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PetjaTheme.spacing.xs),
        modifier = modifier
            .fillMaxWidth()
            .clip(PetjaTheme.shapes.card)
            .background(if (selected) colors.highlightContainer else colors.surfaceContainerLowest)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .heightIn(min = PetjaTheme.sizes.minTouchTarget)
            .padding(PetjaTheme.spacing.s),
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = if (selected) colors.highlight else colors.onSurfaceVariant,
            modifier = Modifier.size(PetjaTheme.sizes.icon),
        )
        // O toque é da linha inteira; o rádio só mostra o estado.
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = PetjaTheme.typography.body, color = colors.onSurface)
            details?.let { Text(text = it, style = PetjaTheme.typography.caption, color = colors.onSurfaceVariant) }
        }
    }
}
