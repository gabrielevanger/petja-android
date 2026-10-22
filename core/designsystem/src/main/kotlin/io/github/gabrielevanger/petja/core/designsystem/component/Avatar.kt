package io.github.gabrielevanger.petja.core.designsystem.component

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
import io.github.gabrielevanger.petja.core.designsystem.R
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme
import io.github.gabrielevanger.petja.core.designsystem.theme.accentContainer
import io.github.gabrielevanger.petja.core.designsystem.theme.onAccentContainer

// Medidas do avatar do Figma (cartão do perfil).
private val AvatarSize = 80.dp
private val AvatarIcon = 36.dp

/** Avatar do cliente enquanto não há foto de perfil: círculo pêssego com o boneco. Decorativo. */
@Composable
fun Avatar(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(AvatarSize).background(PetjaTheme.colors.accentContainer, CircleShape),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_user_round),
            contentDescription = null,
            tint = PetjaTheme.colors.onAccentContainer,
            modifier = Modifier.size(AvatarIcon),
        )
    }
}
