package io.github.gabrielevanger.xodopet.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.gabrielevanger.xodopet.core.designsystem.R
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme
import io.github.gabrielevanger.xodopet.core.designsystem.theme.accentContainer
import io.github.gabrielevanger.xodopet.core.designsystem.theme.onAccentContainer

// Medidas do avatar do Figma (cartão do perfil).
private val AvatarSize = 80.dp
private val AvatarIcon = 36.dp

/** Avatar do cliente enquanto não há foto de perfil: círculo pêssego com o boneco. Decorativo. */
@Composable
fun Avatar(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(AvatarSize).background(XodoTheme.colors.accentContainer, CircleShape),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_user_round),
            contentDescription = null,
            tint = XodoTheme.colors.onAccentContainer,
            modifier = Modifier.size(AvatarIcon),
        )
    }
}
