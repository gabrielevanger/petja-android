package io.github.gabrielevanger.petja.feature.cart

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
import io.github.gabrielevanger.petja.core.designsystem.R as DesignR
import io.github.gabrielevanger.petja.core.designsystem.component.IconTile
import io.github.gabrielevanger.petja.core.designsystem.component.LoadingState
import io.github.gabrielevanger.petja.core.designsystem.component.MessageState
import io.github.gabrielevanger.petja.core.designsystem.component.Notice
import io.github.gabrielevanger.petja.core.designsystem.component.PetjaButton
import io.github.gabrielevanger.petja.core.designsystem.component.PetjaButtonStyle
import io.github.gabrielevanger.petja.core.designsystem.component.PetjaCard
import io.github.gabrielevanger.petja.core.designsystem.component.ProductThumbnail
import io.github.gabrielevanger.petja.core.designsystem.component.QuantityStepper
import io.github.gabrielevanger.petja.core.designsystem.component.SectionTitle
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme
import io.github.gabrielevanger.petja.core.ui.BackTopBar
import io.github.gabrielevanger.petja.core.ui.CollectEvents
import io.github.gabrielevanger.petja.core.ui.dotSeparated

@Composable
fun CartRoute(
    onBack: () -> Unit,
    onCheckout: () -> Unit,
    onSignIn: () -> Unit,
    viewModel: CartViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            CartEvent.Close -> onBack()
            CartEvent.OpenCheckout -> onCheckout()
            CartEvent.OpenSignIn -> onSignIn()
        }
    }
    CartScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun CartScreen(state: CartUiState, onAction: (CartAction) -> Unit) {
    Scaffold(
        containerColor = PetjaTheme.colors.background,
        topBar = { BackTopBar(stringResource(R.string.cart_title), onBack = { onAction(CartAction.OnBackClick) }) },
        bottomBar = { if (state is CartUiState.Loaded) SummaryPanel(state) { onAction(CartAction.OnContinueClick) } },
    ) { padding ->
        val modifier = Modifier.padding(padding).consumeWindowInsets(padding)
        when (state) {
            CartUiState.Loading -> LoadingState(stringResource(R.string.cart_loading), modifier = modifier)
            CartUiState.Empty -> MessageState(stringResource(R.string.cart_empty), modifier = modifier)
            is CartUiState.Loaded -> CartContent(state, onAction, modifier)
        }
    }
}

@Composable
private fun CartContent(state: CartUiState.Loaded, onAction: (CartAction) -> Unit, modifier: Modifier) {
    val spacing = PetjaTheme.spacing
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(spacing.l),
        contentPadding = PaddingValues(horizontal = spacing.l, vertical = spacing.m),
        modifier = modifier,
    ) {
        item(key = "store") { StoreHeader(state) }
        items(state.lines, key = { it.variantId }) { line ->
            CartLineCard(
                line,
                onIncrease = { onAction(CartAction.OnIncrease(line.variantId, line.quantity)) },
                onDecrease = { onAction(CartAction.OnDecrease(line.variantId, line.quantity)) },
            )
        }
        item(key = "clear") {
            PetjaButton(
                text = stringResource(R.string.cart_clear),
                onClick = { onAction(CartAction.OnClearClick) },
                style = PetjaButtonStyle.Text,
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
        horizontalArrangement = Arrangement.spacedBy(PetjaTheme.spacing.s),
    ) {
        IconTile(painterResource(DesignR.drawable.ic_store))
        Column(verticalArrangement = Arrangement.spacedBy(PetjaTheme.spacing.xxs)) {
            Text(text = state.storeName, style = PetjaTheme.typography.sectionTitle)
            Text(
                text = dotSeparated(
                    pluralStringResource(R.plurals.cart_item_count, state.itemCount, state.itemCount),
                    state.placeName?.let { stringResource(R.string.cart_deliver_to, it) },
                ),
                style = PetjaTheme.typography.caption,
                color = PetjaTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CartLineCard(line: CartLineUi, onIncrease: () -> Unit, onDecrease: () -> Unit) {
    val spacing = PetjaTheme.spacing
    PetjaCard(shape = PetjaTheme.shapes.panel) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
            ProductThumbnail()
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.weight(1f)) {
                Text(text = line.productName, style = PetjaTheme.typography.cardTitle)
                Text(
                    text = dotSeparated(line.variantLabel, stringResource(R.string.cart_unit_price, line.unitPrice)),
                    style = PetjaTheme.typography.caption,
                    color = PetjaTheme.colors.onSurfaceVariant,
                )
                Text(text = line.total, style = PetjaTheme.typography.sectionTitle)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = spacing.s)) {
            Text(
                text = stringResource(R.string.cart_quantity),
                style = PetjaTheme.typography.caption,
                color = PetjaTheme.colors.onSurfaceVariant,
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

/** Resumo fixo embaixo, com o botão de seguir; o ViewModel decide se vai para o checkout ou para entrar. */
@Composable
private fun SummaryPanel(state: CartUiState.Loaded, onContinue: () -> Unit) {
    Surface(color = PetjaTheme.colors.surfaceContainerLowest) {
        Column(
            verticalArrangement = Arrangement.spacedBy(PetjaTheme.spacing.s),
            modifier = Modifier.navigationBarsPadding().padding(PetjaTheme.spacing.l),
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
                HorizontalDivider(thickness = PetjaTheme.sizes.divider)
                SummaryRow(stringResource(R.string.cart_total), it, emphasis = true)
            }
            if ((state.delivery as? DeliveryUi.Fee)?.storeIsOpen == false) {
                Notice(stringResource(R.string.cart_store_closed))
            }
            state.missingForMinOrder?.let { Notice(stringResource(R.string.cart_min_order, it, state.minOrder)) }
            PetjaButton(
                text = stringResource(
                    if (state.signedIn) R.string.cart_checkout else R.string.cart_sign_in_to_checkout,
                ),
                onClick = onContinue,
                enabled = state.canCheckout,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, emphasis: Boolean = false) {
    val style = if (emphasis) PetjaTheme.typography.sectionTitle else PetjaTheme.typography.body
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = style, modifier = Modifier.weight(1f))
        Text(text = value, style = if (emphasis) style else PetjaTheme.typography.price)
    }
}
