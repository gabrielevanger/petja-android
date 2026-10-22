package io.github.gabrielevanger.petja.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.gabrielevanger.petja.core.designsystem.R
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme
import io.github.gabrielevanger.petja.core.designsystem.theme.accentContainer
import io.github.gabrielevanger.petja.core.designsystem.theme.onAccentContainer

/** Foto do produto em retrato, como nos cartões do carrinho. */
private val ThumbnailWidth = 86.dp
private val ThumbnailHeight = 100.dp
private val PlaceholderIcon = 34.dp

/**
 * Miniatura do produto. Enquanto os produtos não têm foto (issue #12), mostra o espaço reservado
 * do layout: fundo pêssego com um osso. Decorativa: o nome do produto vem ao lado.
 */
@Composable
fun ProductThumbnail(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(ThumbnailWidth, ThumbnailHeight)
            .clip(PetjaTheme.shapes.card)
            .background(PetjaTheme.colors.accentContainer),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_bone),
            contentDescription = null,
            tint = PetjaTheme.colors.onAccentContainer,
            modifier = Modifier.size(PlaceholderIcon),
        )
    }
}
