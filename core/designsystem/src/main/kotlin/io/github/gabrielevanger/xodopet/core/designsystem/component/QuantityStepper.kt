package io.github.gabrielevanger.xodopet.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import io.github.gabrielevanger.xodopet.core.designsystem.R
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme

/**
 * Menos, quantidade e mais. Com uma unidade, o botão de menos vira lixeira: o próximo toque remove o item.
 * As descrições para o TalkBack vêm de quem chama, com o nome do item.
 */
@Composable
fun QuantityStepper(
    quantity: Int,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseDescription: String,
    increaseDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        IconButton(onClick = onDecrease) {
            Icon(
                painter = painterResource(if (quantity > 1) R.drawable.ic_remove else R.drawable.ic_delete),
                contentDescription = decreaseDescription,
            )
        }
        Text(text = quantity.toString(), style = XodoTheme.typography.price)
        IconButton(onClick = onIncrease, enabled = canIncrease) {
            Icon(painter = painterResource(R.drawable.ic_add), contentDescription = increaseDescription)
        }
    }
}
