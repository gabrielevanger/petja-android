package io.github.gabrielevanger.xodopet.core.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme

/** Título numa linha só: nomes longos terminam em reticências. */
private const val TITLE_MAX_LINES = 1

/** Barra do topo com voltar e título, para telas abertas a partir de outra. */
@Composable
fun BackTopBar(title: String?, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.statusBarsPadding().padding(XodoTheme.spacing.xxs),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
        }
        title?.let {
            Text(
                text = it,
                style = XodoTheme.typography.topBarTitle,
                maxLines = TITLE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
