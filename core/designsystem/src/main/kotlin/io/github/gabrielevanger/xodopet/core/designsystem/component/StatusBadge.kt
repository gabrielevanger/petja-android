package io.github.gabrielevanger.xodopet.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme

enum class BadgeTone { Positive, Neutral }

/** Selo curto de status, como "Aberta" ou "Fechada". Sempre com texto: o status não depende só da cor (RNF-07). */
@Composable
fun StatusBadge(text: String, tone: BadgeTone, modifier: Modifier = Modifier) {
    val colors = XodoTheme.colors
    Surface(
        color = if (tone == BadgeTone.Positive) colors.secondaryContainer else colors.surfaceVariant,
        contentColor = if (tone == BadgeTone.Positive) colors.onSecondaryContainer else colors.onSurfaceVariant,
        shape = XodoTheme.shapes.badge,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = XodoTheme.typography.badge,
            modifier = Modifier.padding(horizontal = XodoTheme.spacing.xs, vertical = XodoTheme.spacing.xxs),
        )
    }
}
