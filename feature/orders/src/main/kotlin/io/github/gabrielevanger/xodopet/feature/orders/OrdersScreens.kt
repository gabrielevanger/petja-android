package io.github.gabrielevanger.xodopet.feature.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gabrielevanger.xodopet.core.designsystem.component.ErrorState
import io.github.gabrielevanger.xodopet.core.designsystem.component.LoadingState
import io.github.gabrielevanger.xodopet.core.designsystem.component.MessageState
import io.github.gabrielevanger.xodopet.core.designsystem.component.Notice
import io.github.gabrielevanger.xodopet.core.designsystem.component.SectionTitle
import io.github.gabrielevanger.xodopet.core.designsystem.component.StatusBadge
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme
import io.github.gabrielevanger.xodopet.core.domain.checkout.PaymentChoice
import io.github.gabrielevanger.xodopet.core.domain.model.Money
import io.github.gabrielevanger.xodopet.core.domain.model.OrderDetails
import io.github.gabrielevanger.xodopet.core.domain.model.OrderSummary
import io.github.gabrielevanger.xodopet.core.ui.BackTopBar
import io.github.gabrielevanger.xodopet.core.ui.R as CoreUiR
import io.github.gabrielevanger.xodopet.core.ui.dotSeparated
import io.github.gabrielevanger.xodopet.core.ui.formatBrl
import io.github.gabrielevanger.xodopet.core.ui.formatDateTime
import io.github.gabrielevanger.xodopet.core.ui.whatsappLink

// ---------------------------------------------------------------------------------------------
// Lista
// ---------------------------------------------------------------------------------------------

@Composable
fun OrdersRoute(
    onBack: () -> Unit,
    onOpenOrder: (String) -> Unit,
    onSignIn: () -> Unit,
    viewModel: OrdersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Ao voltar dos detalhes (cancelou, o status mudou), a lista se atualiza.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }
    OrdersScreen(state, onBack, onOpenOrder, onRetry = viewModel::load, onSignIn = onSignIn)
}

@Composable
fun OrdersScreen(
    state: OrdersUiState,
    onBack: () -> Unit,
    onOpenOrder: (String) -> Unit,
    onRetry: () -> Unit,
    onSignIn: () -> Unit,
) {
    Scaffold(containerColor = XodoTheme.colors.background, topBar = {
        BackTopBar(stringResource(R.string.orders_title), onBack)
    }) { padding ->
        val modifier = Modifier.padding(padding).consumeWindowInsets(padding)
        when (state) {
            OrdersUiState.Loading -> LoadingState(stringResource(R.string.orders_loading), modifier)

            OrdersUiState.Error -> ErrorState(
                stringResource(R.string.orders_error),
                stringResource(CoreUiR.string.common_retry),
                onRetry,
                modifier,
            )

            OrdersUiState.NotSignedIn -> ErrorState(
                stringResource(R.string.orders_not_signed_in),
                stringResource(R.string.order_sign_in),
                onSignIn,
                modifier,
            )

            is OrdersUiState.Loaded -> if (state.orders.isEmpty()) {
                MessageState(stringResource(R.string.orders_empty), modifier)
            } else {
                LazyColumn(
                    modifier = modifier,
                    contentPadding = PaddingValues(XodoTheme.spacing.m),
                    verticalArrangement = Arrangement.spacedBy(XodoTheme.spacing.xs),
                ) {
                    items(state.orders, key = {
                        it.id
                    }) { order -> OrderCard(order, onClick = { onOpenOrder(order.id) }) }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(order: OrderSummary, onClick: () -> Unit) {
    Card(onClick = onClick, shape = XodoTheme.shapes.card, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(XodoTheme.spacing.m),
            verticalArrangement = Arrangement.spacedBy(XodoTheme.spacing.xxs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.orders_number_store, order.number, order.storeName),
                    style = XodoTheme.typography.cardTitle,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(order.status.label(), order.status.tone)
            }
            Text(
                text = dotSeparated(
                    formatDateTime(order.createdAt),
                    pluralStringResource(R.plurals.orders_items, order.itemCount, order.itemCount),
                    order.total.formatBrl(),
                ),
                style = XodoTheme.typography.caption,
                color = XodoTheme.colors.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Detalhes
// ---------------------------------------------------------------------------------------------

@Composable
fun OrderDetailsRoute(onBack: () -> Unit, onOpenCart: () -> Unit, viewModel: OrderDetailsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.openCart.collect { onOpenCart() } }
    OrderDetailsScreen(
        state,
        OrderDetailsActions(
            onBack = onBack,
            onRetry = viewModel::retry,
            onAskCancel = viewModel::askToCancel,
            onConfirmCancel = viewModel::confirmCancel,
            onDismissCancel = viewModel::dismissCancel,
            onReorder = viewModel::reorder,
            onMessageShown = viewModel::messageShown,
        ),
    )
}

class OrderDetailsActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onAskCancel: () -> Unit = {},
    val onConfirmCancel: () -> Unit = {},
    val onDismissCancel: () -> Unit = {},
    val onReorder: () -> Unit = {},
    val onMessageShown: () -> Unit = {},
)

@Composable
fun OrderDetailsScreen(state: OrderDetailsUiState, actions: OrderDetailsActions) {
    val snackbar = remember { SnackbarHostState() }
    val loaded = state as? OrderDetailsUiState.Loaded
    loaded?.message?.let { message ->
        val text = message.text()
        LaunchedEffect(message) {
            snackbar.showSnackbar(text)
            actions.onMessageShown()
        }
    }
    if (loaded?.confirmingCancel == true) {
        AlertDialog(
            onDismissRequest = actions.onDismissCancel,
            title = { Text(stringResource(R.string.order_cancel_title)) },
            text = { Text(stringResource(R.string.order_cancel_text)) },
            confirmButton = {
                TextButton(onClick = actions.onConfirmCancel) { Text(stringResource(R.string.order_cancel_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = actions.onDismissCancel) { Text(stringResource(R.string.order_cancel_dismiss)) }
            },
        )
    }
    Scaffold(
        containerColor = XodoTheme.colors.background,
        topBar = { BackTopBar(loaded?.let { stringResource(R.string.order_title, it.order.number) }, actions.onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val modifier = Modifier.padding(padding).consumeWindowInsets(padding)
        when (state) {
            OrderDetailsUiState.Loading -> LoadingState(stringResource(R.string.order_loading), modifier)

            OrderDetailsUiState.Error -> ErrorState(
                stringResource(R.string.order_error),
                stringResource(CoreUiR.string.common_retry),
                actions.onRetry,
                modifier,
            )

            OrderDetailsUiState.NotFound -> MessageState(stringResource(R.string.order_not_found), modifier)

            is OrderDetailsUiState.Loaded -> Column(
                verticalArrangement = Arrangement.spacedBy(XodoTheme.spacing.s),
                modifier = modifier.verticalScroll(rememberScrollState()).padding(XodoTheme.spacing.m),
            ) { OrderContent(state, actions) }
        }
    }
}

@Composable
private fun ColumnScope.OrderContent(state: OrderDetailsUiState.Loaded, actions: OrderDetailsActions) {
    val order = state.order
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.order_from_store, order.storeName, formatDateTime(order.createdAt)),
            style = XodoTheme.typography.body,
            modifier = Modifier.weight(1f),
        )
        StatusBadge(order.status.label(), order.status.tone)
    }
    order.statusReason?.let { Notice(it) }
    if (!order.status.isFinal) {
        Text(
            text = stringResource(R.string.order_live),
            style = XodoTheme.typography.caption,
            color = XodoTheme.colors.onSurfaceVariant,
        )
    }
    order.courierName?.let { Text(stringResource(R.string.order_courier, it), style = XodoTheme.typography.body) }
    Actions(state, actions)
    Timeline(order)
    Items(order)
    Values(order)
    SectionTitle(stringResource(R.string.order_payment_title))
    Text(text = order.payment.label(), style = XodoTheme.typography.body)
    SectionTitle(stringResource(R.string.order_address_title))
    Text(
        text = stringResource(R.string.order_address_line, order.address.street, order.address.number),
        style = XodoTheme.typography.body,
    )
    dotSeparated(order.address.complement, order.address.neighborhood, order.address.reference).takeIf {
        it.isNotEmpty()
    }?.let {
        Text(text = it, style = XodoTheme.typography.caption, color = XodoTheme.colors.onSurfaceVariant)
    }
    order.notes?.let {
        SectionTitle(stringResource(R.string.order_notes_title))
        Text(text = it, style = XodoTheme.typography.body)
    }
}

@Composable
private fun Actions(state: OrderDetailsUiState.Loaded, actions: OrderDetailsActions) {
    val uriHandler = LocalUriHandler.current
    val order = state.order
    if (order.status.customerCanCancel) {
        OutlinedButton(onClick = actions.onAskCancel, enabled = !state.working, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.order_cancel))
        }
    }
    order.storeWhatsapp?.let { number ->
        OutlinedButton(onClick = { uriHandler.openUri(whatsappLink(number)) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.order_whatsapp))
        }
    }
    Button(onClick = actions.onReorder, enabled = !state.working, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.order_reorder))
    }
}

@Composable
private fun Timeline(order: OrderDetails) {
    SectionTitle(stringResource(R.string.order_timeline_title))
    order.history.forEach { change ->
        Column {
            Row {
                Text(text = change.status.label(), style = XodoTheme.typography.body, modifier = Modifier.weight(1f))
                Text(
                    text = formatDateTime(change.at),
                    style = XodoTheme.typography.caption,
                    color = XodoTheme.colors.onSurfaceVariant,
                )
            }
            change.reason?.let {
                Text(text = it, style = XodoTheme.typography.caption, color = XodoTheme.colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Items(order: OrderDetails) {
    SectionTitle(stringResource(R.string.order_items_title))
    order.items.forEach { item ->
        Row {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.order_item_line, item.quantity, item.productName),
                    style = XodoTheme.typography.body,
                )
                Text(
                    text = item.variantLabel,
                    style = XodoTheme.typography.caption,
                    color = XodoTheme.colors.onSurfaceVariant,
                )
            }
            Text(text = item.total.formatBrl(), style = XodoTheme.typography.price)
        }
    }
}

@Composable
private fun Values(order: OrderDetails) {
    SectionTitle(stringResource(R.string.order_values_title))
    ValueRow(stringResource(R.string.order_subtotal), order.subtotal.formatBrl())
    ValueRow(
        stringResource(R.string.order_delivery),
        if (order.deliveryFee ==
            Money.ZERO
        ) {
            stringResource(R.string.order_delivery_free)
        } else {
            order.deliveryFee.formatBrl()
        },
    )
    ValueRow(stringResource(R.string.order_total), order.total.formatBrl())
}

@Composable
private fun ValueRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = XodoTheme.typography.body, modifier = Modifier.weight(1f))
        Text(text = value, style = XodoTheme.typography.price)
    }
}

@Composable
private fun PaymentChoice.label(): String = when (this) {
    is PaymentChoice.Cash -> changeFor?.let { stringResource(R.string.order_payment_cash_change, it.formatBrl()) }
        ?: stringResource(R.string.order_payment_cash)

    PaymentChoice.PixOnDelivery -> stringResource(R.string.order_payment_pix)

    PaymentChoice.CardOnDelivery -> stringResource(R.string.order_payment_card)
}

@Composable
private fun OrderMessage.text(): String = stringResource(
    when (this) {
        OrderMessage.CannotCancel -> R.string.order_cannot_cancel
        OrderMessage.ReorderSomeChanged -> R.string.order_reorder_some_changed
        OrderMessage.ReorderNothing -> R.string.order_reorder_nothing
        OrderMessage.ActionFailed -> R.string.order_action_failed
    },
)
