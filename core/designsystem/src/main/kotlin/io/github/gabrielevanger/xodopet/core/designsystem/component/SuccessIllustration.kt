package io.github.gabrielevanger.xodopet.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.gabrielevanger.xodopet.core.designsystem.R
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme
import io.github.gabrielevanger.xodopet.core.designsystem.theme.highlight
import io.github.gabrielevanger.xodopet.core.designsystem.theme.highlightContainer
import io.github.gabrielevanger.xodopet.core.designsystem.theme.onHighlight

// Medidas da ilustração do Figma ("Pedido recebido"): só valem aqui dentro.
private val Halo = 188.dp
private val Tile = 110.dp
private val TileCorner = 36.dp
private val TileIcon = 54.dp
private val Check = 46.dp
private val CheckIcon = 28.dp

// Posição do selo no Figma (136, 132), contada a partir do centro, onde o Box o coloca.
private val CheckOffsetX = 136.dp - (Halo - Check) / 2
private val CheckOffsetY = 132.dp - (Halo - Check) / 2

/**
 * Ilustração de sucesso: o ícone num quadrado verde dentro de um halo claro, com o selo de
 * confirmação no canto. Decorativa; o texto ao lado diz o que aconteceu.
 */
@Composable
fun SuccessIllustration(icon: Painter, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        // Sem recorte: o selo passa da borda do halo, como no layout.
        modifier = modifier.size(Halo).background(XodoTheme.colors.highlightContainer, CircleShape),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Tile)
                .clip(RoundedCornerShape(TileCorner))
                .background(XodoTheme.colors.highlight),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = XodoTheme.colors.onHighlight,
                modifier = Modifier.size(TileIcon),
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(CheckOffsetX, CheckOffsetY)
                .size(Check)
                .clip(CircleShape)
                .background(XodoTheme.colors.primary),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = XodoTheme.colors.onPrimary,
                modifier = Modifier.size(CheckIcon),
            )
        }
    }
}
