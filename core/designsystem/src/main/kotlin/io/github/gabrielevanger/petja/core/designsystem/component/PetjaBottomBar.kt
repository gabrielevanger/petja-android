package io.github.gabrielevanger.petja.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme

/** Um destino da barra inferior (Vitrine, Pedidos, Conta). */
@Immutable
data class BottomBarItem(val label: String, val icon: Painter, val selected: Boolean, val onClick: () -> Unit)

/**
 * Barra inferior do layout: ícone de traço com o rótulo embaixo, o destino atual em dourado e
 * negrito. Fica acima da barra de navegação do sistema.
 */
@Composable
fun PetjaBottomBar(items: List<BottomBarItem>, modifier: Modifier = Modifier) {
    Surface(color = PetjaTheme.colors.background, modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .navigationBarsPadding()
                .selectableGroup()
                .padding(horizontal = PetjaTheme.spacing.l, vertical = PetjaTheme.spacing.xxs),
        ) {
            items.forEach { item -> BottomBarDestination(item) }
        }
    }
}

@Composable
private fun BottomBarDestination(item: BottomBarItem) {
    val color = if (item.selected) PetjaTheme.colors.primary else PetjaTheme.colors.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PetjaTheme.spacing.xxs, Alignment.CenterVertically),
        modifier = Modifier
            .width(PetjaTheme.sizes.bottomBarItem)
            .heightIn(min = PetjaTheme.sizes.minTouchTarget)
            .selectable(selected = item.selected, role = Role.Tab, onClick = item.onClick)
            .padding(vertical = PetjaTheme.spacing.xs),
    ) {
        Icon(
            painter = item.icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(PetjaTheme.sizes.icon),
        )
        Text(
            text = item.label,
            style = if (item.selected) PetjaTheme.typography.navLabelSelected else PetjaTheme.typography.navLabel,
            color = color,
        )
    }
}
