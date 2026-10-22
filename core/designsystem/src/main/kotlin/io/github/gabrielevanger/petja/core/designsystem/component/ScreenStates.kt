package io.github.gabrielevanger.petja.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme

/** Estados de tela inteira. Os textos vêm de quem chama: o design system não tem texto próprio. */
@Composable
fun LoadingState(message: String, modifier: Modifier = Modifier) {
    CenteredState(modifier) {
        CircularProgressIndicator()
        StateMessage(message, Modifier.padding(top = PetjaTheme.spacing.m))
    }
}

@Composable
fun ErrorState(message: String, actionLabel: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    CenteredState(modifier) {
        StateMessage(message)
        Button(onClick = onAction, modifier = Modifier.padding(top = PetjaTheme.spacing.m)) {
            Text(actionLabel)
        }
    }
}

@Composable
fun MessageState(message: String, modifier: Modifier = Modifier) {
    CenteredState(modifier) { StateMessage(message) }
}

@Composable
private fun CenteredState(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(PetjaTheme.spacing.l),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

@Composable
private fun StateMessage(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = PetjaTheme.typography.message,
        color = PetjaTheme.colors.onBackground,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}
