package io.github.gabrielevanger.petja.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme

/** Resumo do carrinho para a barra de baixo; null quando o carrinho está vazio. */
@Immutable
data class CartBarUi(val storeName: String, val itemCount: Int, val subtotal: String)

/** Barra fixa embaixo da tela com o resumo do carrinho e o atalho para ele. */
@Composable
fun CartBar(cart: CartBarUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = PetjaTheme.spacing
    Surface(color = PetjaTheme.colors.surfaceContainerHigh, modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.navigationBarsPadding().padding(horizontal = spacing.m, vertical = spacing.xs),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dotSeparated(
                        pluralStringResource(R.plurals.cart_bar_items, cart.itemCount, cart.itemCount),
                        cart.subtotal,
                    ),
                    style = PetjaTheme.typography.price,
                )
                Text(
                    text = cart.storeName,
                    style = PetjaTheme.typography.caption,
                    color = PetjaTheme.colors.onSurfaceVariant,
                )
            }
            Button(onClick = onClick) { Text(stringResource(R.string.cart_bar_action)) }
        }
    }
}
