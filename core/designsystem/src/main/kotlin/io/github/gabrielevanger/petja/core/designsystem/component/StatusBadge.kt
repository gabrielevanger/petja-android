package io.github.gabrielevanger.petja.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme

enum class BadgeTone { Positive, Neutral }

/** Selo curto de status, como "Aberta" ou "Fechada". Sempre com texto: o status não depende só da cor (RNF-07). */
@Composable
fun StatusBadge(text: String, tone: BadgeTone, modifier: Modifier = Modifier) {
    val colors = PetjaTheme.colors
    Surface(
        color = if (tone == BadgeTone.Positive) colors.secondaryContainer else colors.surfaceVariant,
        contentColor = if (tone == BadgeTone.Positive) colors.onSecondaryContainer else colors.onSurfaceVariant,
        shape = PetjaTheme.shapes.badge,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = PetjaTheme.typography.badge,
            modifier = Modifier.padding(horizontal = PetjaTheme.spacing.xs, vertical = PetjaTheme.spacing.xxs),
        )
    }
}
