package io.github.gabrielevanger.xodopet.feature.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gabrielevanger.xodopet.core.designsystem.R as DesignR
import io.github.gabrielevanger.xodopet.core.designsystem.component.IconTile
import io.github.gabrielevanger.xodopet.core.designsystem.component.LoadingState
import io.github.gabrielevanger.xodopet.core.designsystem.component.MessageState
import io.github.gabrielevanger.xodopet.core.designsystem.component.Notice
import io.github.gabrielevanger.xodopet.core.designsystem.component.ProductThumbnail
import io.github.gabrielevanger.xodopet.core.designsystem.component.QuantityStepper
import io.github.gabrielevanger.xodopet.core.designsystem.component.SectionTitle
import io.github.gabrielevanger.xodopet.core.designsystem.component.XodoButton
import io.github.gabrielevanger.xodopet.core.designsystem.component.XodoButtonStyle
import io.github.gabrielevanger.xodopet.core.designsystem.component.XodoCard
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme
import io.github.gabrielevanger.xodopet.core.ui.BackTopBar
import io.github.gabrielevanger.xodopet.core.ui.dotSeparated

@Composable
fun CartRoute(
    onBack: () -> Unit,
    onCheckout: () -> Unit,
    onSignIn: () -> Unit,
    viewModel: CartViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CartScreen(
        state = state,
        onBack = onBack,
        onIncrease = viewModel::increase,
        onDecrease = viewModel::decrease,
        onClear = viewModel::clear,
        onCheckout = onCheckout,
        onSignIn = onSignIn,
    )
}

@Composable
fun CartScreen(
    state: CartUiState,
    onBack: () -> Unit,
    onIncrease: (variantId: String, quantity: Int) -> Unit,
    onDecrease: (variantId: String, quantity: Int) -> Unit,
    onClear: () -> Unit,
    onCheckout: () -> Unit = {},
    onSignIn: () -> Unit = {},
) {
    Scaffold(
        containerColor = XodoTheme.colors.background,
        topBar = { BackTopBar(stringResource(R.string.cart_title), onBack) },
        bottomBar = { if (state is CartUiState.Loaded) SummaryPanel(state, onCheckout, onSignIn) },
    ) { padding ->
        val modifier = Modifier.padding(padding).consumeWindowInsets(padding)
        when (state) {
            CartUiState.Loading -> LoadingState(stringResource(R.string.cart_loading), modifier = modifier)
            CartUiState.Empty -> MessageState(stringResource(R.string.cart_empty), modifier = modifier)
            is CartUiState.Loaded -> CartContent(state, onIncrease, onDecrease, onClear, modifier)
        }
    }
}

@Composable
private fun CartContent(
    state: CartUiState.Loaded,
    onIncrease: (String, Int) -> Unit,
    onDecrease: (String, Int) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier,
) {
    val spacing = XodoTheme.spacing
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(spacing.l),
        contentPadding = PaddingValues(horizontal = spacing.l, vertical = spacing.m),
        modifier = modifier,
    ) {
        item(key = "store") { StoreHeader(state) }
        items(state.lines, key = { it.variantId }) { line ->
            CartLineCard(line, onIncrease = { onIncrease(line.variantId, line.quantity) }, onDecrease = {
                onDecrease(line.variantId, line.quantity)
            })
        }
        item(key = "clear") {
            XodoButton(
                text = stringResource(R.string.cart_clear),
                onClick = onClear,
                style = XodoButtonStyle.Text,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Loja do carrinho e quantos itens tem, com o lugar de entrega quando já foi escolhido. */
@Composable
private fun StoreHeader(state: CartUiState.Loaded) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(XodoTheme.spacing.s),
    ) {
        IconTile(painterResource(DesignR.drawable.ic_store))
        Column(verticalArrangement = Arrangement.spacedBy(XodoTheme.spacing.xxs)) {
            Text(text = state.storeName, style = XodoTheme.typography.sectionTitle)
            Text(
                text = dotSeparated(
                    pluralStringResource(R.plurals.cart_item_count, state.itemCount, state.itemCount),
                    state.placeName?.let { stringResource(R.string.cart_deliver_to, it) },
                ),
                style = XodoTheme.typography.caption,
                color = XodoTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CartLineCard(line: CartLineUi, onIncrease: () -> Unit, onDecrease: () -> Unit) {
    val spacing = XodoTheme.spacing
    XodoCard(shape = XodoTheme.shapes.panel) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
            ProductThumbnail()
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.weight(1f)) {
                Text(text = line.productName, style = XodoTheme.typography.cardTitle)
                Text(
                    text = dotSeparated(line.variantLabel, stringResource(R.string.cart_unit_price, line.unitPrice)),
                    style = XodoTheme.typography.caption,
                    color = XodoTheme.colors.onSurfaceVariant,
                )
                Text(text = line.total, style = XodoTheme.typography.sectionTitle)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = spacing.s)) {
            Text(
                text = stringResource(R.string.cart_quantity),
                style = XodoTheme.typography.caption,
                color = XodoTheme.colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            QuantityStepper(
                quantity = line.quantity,
                canIncrease = line.canIncrease,
                onDecrease = onDecrease,
                onIncrease = onIncrease,
                decreaseDescription = stringResource(
                    if (line.quantity > 1) R.string.cart_decrease else R.string.cart_remove,
                    line.productName,
                ),
                increaseDescription = stringResource(R.string.cart_increase, line.productName),
            )
        }
    }
}

/**
 * Resumo fixo embaixo, com o botão de seguir. Sem login, o botão leva para entrar e volta para o
 * carrinho; logado, segue para o checkout.
 */
@Composable
private fun SummaryPanel(state: CartUiState.Loaded, onCheckout: () -> Unit, onSignIn: () -> Unit) {
    Surface(color = XodoTheme.colors.surfaceContainerLowest) {
        Column(
            verticalArrangement = Arrangement.spacedBy(XodoTheme.spacing.s),
            modifier = Modifier.navigationBarsPadding().padding(XodoTheme.spacing.l),
        ) {
            SectionTitle(stringResource(R.string.cart_summary_title))
            SummaryRow(stringResource(R.string.cart_subtotal), state.subtotal)
            when (val delivery = state.delivery) {
                DeliveryUi.Calculating -> SummaryRow(
                    stringResource(R.string.cart_delivery),
                    stringResource(R.string.cart_delivery_calculating),
                )

                is DeliveryUi.Fee -> SummaryRow(
                    stringResource(R.string.cart_delivery),
                    if (delivery.isFree) stringResource(R.string.cart_delivery_free) else delivery.value,
                )

                DeliveryUi.NoPlace -> Notice(stringResource(R.string.cart_delivery_no_place))

                DeliveryUi.OutOfArea -> Notice(
                    stringResource(R.string.cart_delivery_out_of_area, state.placeName.orEmpty()),
                )

                DeliveryUi.Error -> Notice(stringResource(R.string.cart_delivery_error))
            }
            state.total?.let {
                HorizontalDivider(thickness = XodoTheme.sizes.divider)
                SummaryRow(stringResource(R.string.cart_total), it, emphasis = true)
            }
            if ((state.delivery as? DeliveryUi.Fee)?.storeIsOpen == false) {
                Notice(stringResource(R.string.cart_store_closed))
            }
            state.missingForMinOrder?.let { Notice(stringResource(R.string.cart_min_order, it, state.minOrder)) }
            XodoButton(
                text = stringResource(
                    if (state.signedIn) R.string.cart_checkout else R.string.cart_sign_in_to_checkout,
                ),
                onClick = if (state.signedIn) onCheckout else onSignIn,
                enabled = state.canCheckout,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, emphasis: Boolean = false) {
    val style = if (emphasis) XodoTheme.typography.sectionTitle else XodoTheme.typography.body
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = style, modifier = Modifier.weight(1f))
        Text(text = value, style = if (emphasis) style else XodoTheme.typography.price)
    }
}
