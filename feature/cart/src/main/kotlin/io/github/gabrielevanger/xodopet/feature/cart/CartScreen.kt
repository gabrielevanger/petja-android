package io.github.gabrielevanger.xodopet.feature.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gabrielevanger.xodopet.core.designsystem.component.LoadingState
import io.github.gabrielevanger.xodopet.core.designsystem.component.MessageState
import io.github.gabrielevanger.xodopet.core.designsystem.component.Notice
import io.github.gabrielevanger.xodopet.core.designsystem.component.QuantityStepper
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme
import io.github.gabrielevanger.xodopet.core.ui.R as CoreUiR
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
        topBar = { TopBar(onBack) },
        bottomBar = { if (state is CartUiState.Loaded) CheckoutBar(state, onCheckout, onSignIn) },
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
private fun TopBar(onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.statusBarsPadding().padding(XodoTheme.spacing.xxs),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(CoreUiR.string.common_back))
        }
        Text(text = stringResource(R.string.cart_title), style = XodoTheme.typography.screenTitle)
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
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = spacing.l)) {
        item(key = "store") {
            Column(modifier = Modifier.padding(horizontal = spacing.m, vertical = spacing.xs)) {
                Text(
                    text = stringResource(R.string.cart_from_store, state.storeName),
                    style = XodoTheme.typography.sectionTitle,
                )
                state.placeName?.let {
                    Text(
                        text = stringResource(R.string.cart_deliver_to, it),
                        style = XodoTheme.typography.caption,
                        color = XodoTheme.colors.onSurfaceVariant,
                    )
                }
            }
        }
        items(state.lines, key = { it.variantId }) { line ->
            CartLineRow(line, onIncrease = { onIncrease(line.variantId, line.quantity) }, onDecrease = {
                onDecrease(line.variantId, line.quantity)
            })
        }
        item(key = "summary") { Summary(state) }
        item(key = "clear") {
            TextButton(onClick = onClear, modifier = Modifier.padding(horizontal = spacing.xs)) {
                Text(stringResource(R.string.cart_clear))
            }
        }
    }
}

@Composable
private fun CartLineRow(line: CartLineUi, onIncrease: () -> Unit, onDecrease: () -> Unit) {
    val spacing = XodoTheme.spacing
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(
            start = spacing.m,
            end = spacing.xs,
            top = spacing.xs,
            bottom = spacing.xs,
        ),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = line.productName, style = XodoTheme.typography.itemName)
            Text(
                text = dotSeparated(line.variantLabel, stringResource(R.string.cart_unit_price, line.unitPrice)),
                style = XodoTheme.typography.caption,
                color = XodoTheme.colors.onSurfaceVariant,
            )
            Text(text = line.total, style = XodoTheme.typography.price)
        }
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
    HorizontalDivider(thickness = XodoTheme.sizes.divider, modifier = Modifier.padding(horizontal = spacing.m))
}

@Composable
private fun Summary(state: CartUiState.Loaded) {
    val spacing = XodoTheme.spacing
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        modifier = Modifier.padding(horizontal = spacing.m, vertical = spacing.m),
    ) {
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
        state.total?.let { SummaryRow(stringResource(R.string.cart_total), it, emphasis = true) }
        if ((state.delivery as? DeliveryUi.Fee)?.storeIsOpen == false) {
            Notice(stringResource(R.string.cart_store_closed))
        }
        state.missingForMinOrder?.let { Notice(stringResource(R.string.cart_min_order, it, state.minOrder)) }
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

/** Sem login, o botão leva para entrar e volta para o carrinho; logado, segue para o checkout. */
@Composable
private fun CheckoutBar(state: CartUiState.Loaded, onCheckout: () -> Unit, onSignIn: () -> Unit) {
    Surface(color = XodoTheme.colors.surfaceContainerHigh) {
        Button(
            onClick = if (state.signedIn) onCheckout else onSignIn,
            enabled = state.canCheckout,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(XodoTheme.spacing.m),
        ) {
            Text(stringResource(if (state.signedIn) R.string.cart_checkout else R.string.cart_sign_in_to_checkout))
        }
    }
}
