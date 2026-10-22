package io.github.gabrielevanger.petja.feature.orders

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gabrielevanger.petja.core.designsystem.R as DesignR
import io.github.gabrielevanger.petja.core.designsystem.component.ErrorState
import io.github.gabrielevanger.petja.core.designsystem.component.IconTile
import io.github.gabrielevanger.petja.core.designsystem.component.LoadingState
import io.github.gabrielevanger.petja.core.designsystem.component.MessageState
import io.github.gabrielevanger.petja.core.designsystem.component.Notice
import io.github.gabrielevanger.petja.core.designsystem.component.PetjaButton
import io.github.gabrielevanger.petja.core.designsystem.component.PetjaButtonStyle
import io.github.gabrielevanger.petja.core.designsystem.component.PetjaCard
import io.github.gabrielevanger.petja.core.designsystem.component.SectionTitle
import io.github.gabrielevanger.petja.core.designsystem.component.StatusBadge
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme
import io.github.gabrielevanger.petja.core.designsystem.theme.highlight
import io.github.gabrielevanger.petja.core.designsystem.theme.highlightContainer
import io.github.gabrielevanger.petja.core.designsystem.theme.onHighlight
import io.github.gabrielevanger.petja.core.domain.checkout.PaymentChoice
import io.github.gabrielevanger.petja.core.domain.model.Money
import io.github.gabrielevanger.petja.core.domain.model.OrderDetails
import io.github.gabrielevanger.petja.core.domain.model.OrderStatus
import io.github.gabrielevanger.petja.core.domain.model.OrderSummary
import io.github.gabrielevanger.petja.core.ui.BackTopBar
import io.github.gabrielevanger.petja.core.ui.R as CoreUiR
import io.github.gabrielevanger.petja.core.ui.dotSeparated
import io.github.gabrielevanger.petja.core.ui.formatBrl
import io.github.gabrielevanger.petja.core.ui.formatDateTime
import io.github.gabrielevanger.petja.core.ui.whatsappLink

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
    Scaffold(containerColor = PetjaTheme.colors.background, topBar = {
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
                OrdersList(state, onOpenOrder, modifier)
            }
        }
    }
}

/** Pedidos em andamento em destaque no topo; os encerrados embaixo, em "Anteriores". */
@Composable
private fun OrdersList(state: OrdersUiState.Loaded, onOpenOrder: (String) -> Unit, modifier: Modifier) {
    val spacing = PetjaTheme.spacing
    val (active, past) = state.orders.partition { !it.status.isFinal }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = spacing.l, vertical = spacing.m),
        verticalArrangement = Arrangement.spacedBy(spacing.s),
    ) {
        item(key = "headline") {
            Text(
                text = stringResource(R.string.orders_headline),
                style = PetjaTheme.typography.headline,
                modifier = Modifier.padding(bottom = spacing.s),
            )
        }
        if (active.isNotEmpty()) {
            item(key = "active") { SectionTitle(stringResource(R.string.orders_active_title)) }
            items(active, key = { it.id }) { order -> OrderCard(order, onClick = { onOpenOrder(order.id) }) }
        }
        if (past.isNotEmpty()) {
            item(key = "past") {
                SectionTitle(
                    stringResource(R.string.orders_past_title),
                    modifier = Modifier.padding(top = if (active.isEmpty()) spacing.none else spacing.s),
                )
            }
            items(past, key = { it.id }) { order -> OrderCard(order, onClick = { onOpenOrder(order.id) }) }
        }
    }
}

@Composable
private fun OrderCard(order: OrderSummary, onClick: () -> Unit) {
    val active = !order.status.isFinal
    PetjaCard(
        onClick = onClick,
        shape = PetjaTheme.shapes.panel,
        color = if (active) PetjaTheme.colors.highlightContainer else PetjaTheme.colors.surfaceContainerLowest,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = PetjaTheme.spacing.s),
        ) {
            OrderStatusTile(order.status, modifier = Modifier.weight(1f, fill = false))
            Spacer(Modifier.weight(1f))
            Icon(
                painter = painterResource(DesignR.drawable.ic_chevron_right),
                contentDescription = null,
                tint = PetjaTheme.colors.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.orders_number_store, order.number, order.storeName),
                style = PetjaTheme.typography.cardTitle,
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
            style = PetjaTheme.typography.caption,
            color = PetjaTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(top = PetjaTheme.spacing.xxs),
        )
    }
}

/** Ícone da situação: em andamento em verde forte; encerrados em tom neutro. */
@Composable
private fun OrderStatusTile(status: OrderStatus, modifier: Modifier = Modifier) {
    val active = !status.isFinal
    IconTile(
        icon = painterResource(status.icon),
        containerColor = if (active) PetjaTheme.colors.highlight else PetjaTheme.colors.surfaceContainer,
        contentColor = if (active) PetjaTheme.colors.onHighlight else PetjaTheme.colors.onSurfaceVariant,
        modifier = modifier,
    )
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
        containerColor = PetjaTheme.colors.background,
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
                verticalArrangement = Arrangement.spacedBy(PetjaTheme.spacing.l),
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PetjaTheme.spacing.l, vertical = PetjaTheme.spacing.m),
            ) { OrderContent(state, actions) }
        }
    }
}

@Composable
private fun OrderContent(state: OrderDetailsUiState.Loaded, actions: OrderDetailsActions) {
    val order = state.order
    StatusHeader(order)
    order.statusReason?.let { Notice(it) }
    Actions(state, actions)
    Section(stringResource(R.string.order_timeline_title)) { Timeline(order) }
    Section(stringResource(R.string.order_items_title)) { Items(order) }
    Section(stringResource(R.string.order_delivery_payment_title)) { DeliveryAndPayment(order) }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(PetjaTheme.spacing.s)) {
        SectionTitle(title)
        PetjaCard(shape = PetjaTheme.shapes.panel) {
            Column(verticalArrangement = Arrangement.spacedBy(PetjaTheme.spacing.s), content = content)
        }
    }
}

/** Cartão verde no topo: situação, frase do momento, loja e quem entrega. */
@Composable
private fun StatusHeader(order: OrderDetails) {
    val onHighlight = PetjaTheme.colors.onHighlight
    PetjaCard(
        color = PetjaTheme.colors.highlight,
        shape = PetjaTheme.shapes.panel,
        contentPadding = PetjaTheme.spacing.l,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PetjaTheme.spacing.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(order.status.label(), order.status.tone)
                Spacer(Modifier.weight(1f))
                Icon(painter = painterResource(order.status.icon), contentDescription = null, tint = onHighlight)
            }
            Text(text = order.status.headline(), style = PetjaTheme.typography.headline, color = onHighlight)
            Column(verticalArrangement = Arrangement.spacedBy(PetjaTheme.spacing.xxs)) {
                Text(
                    text = dotSeparated(order.storeName, formatDateTime(order.createdAt)),
                    style = PetjaTheme.typography.body,
                    color = onHighlight,
                )
                order.courierName?.let {
                    Text(
                        stringResource(R.string.order_courier, it),
                        style = PetjaTheme.typography.body,
                        color = onHighlight,
                    )
                }
                if (!order.status.isFinal) {
                    Text(
                        text = stringResource(R.string.order_live),
                        style = PetjaTheme.typography.caption,
                        color = onHighlight,
                    )
                }
            }
        }
    }
}

@Composable
private fun Actions(state: OrderDetailsUiState.Loaded, actions: OrderDetailsActions) {
    val uriHandler = LocalUriHandler.current
    val order = state.order
    Column(verticalArrangement = Arrangement.spacedBy(PetjaTheme.spacing.xs)) {
        PetjaButton(
            text = stringResource(R.string.order_reorder),
            onClick = actions.onReorder,
            enabled = !state.working,
            modifier = Modifier.fillMaxWidth(),
        )
        order.storeWhatsapp?.let { number ->
            PetjaButton(
                text = stringResource(R.string.order_whatsapp),
                onClick = { uriHandler.openUri(whatsappLink(number)) },
                style = PetjaButtonStyle.Tonal,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (order.status.customerCanCancel) {
            PetjaButton(
                text = stringResource(R.string.order_cancel),
                onClick = actions.onAskCancel,
                enabled = !state.working,
                style = PetjaButtonStyle.Outlined,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Cada mudança de status com a hora; a última, que é a atual, em destaque. */
@Composable
private fun Timeline(order: OrderDetails) {
    order.history.forEachIndexed { index, change ->
        val current = index == order.history.lastIndex
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PetjaTheme.spacing.s),
        ) {
            IconTile(
                icon = painterResource(if (current) change.status.icon else DesignR.drawable.ic_check),
                containerColor = if (current) PetjaTheme.colors.highlight else PetjaTheme.colors.highlightContainer,
                contentColor = if (current) PetjaTheme.colors.onHighlight else PetjaTheme.colors.highlight,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = change.status.label(),
                    style = if (current) PetjaTheme.typography.label else PetjaTheme.typography.body,
                )
                change.reason?.let {
                    Text(text = it, style = PetjaTheme.typography.caption, color = PetjaTheme.colors.onSurfaceVariant)
                }
            }
            Text(
                text = formatDateTime(change.at),
                style = PetjaTheme.typography.caption,
                color = PetjaTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Items(order: OrderDetails) {
    order.items.forEach { item ->
        ValueRow(
            stringResource(R.string.order_item_line, item.quantity, item.productName, item.variantLabel),
            item.total.formatBrl(),
        )
    }
    HorizontalDivider(thickness = PetjaTheme.sizes.divider)
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
    ValueRow(stringResource(R.string.order_total), order.total.formatBrl(), emphasis = true)
}

@Composable
private fun DeliveryAndPayment(order: OrderDetails) {
    val address = order.address
    InfoRow(
        DesignR.drawable.ic_map_pin,
        dotSeparated(
            stringResource(R.string.order_address_line, address.street, address.number),
            address.complement,
            address.neighborhood,
            address.reference,
        ),
    )
    val paymentIcon = when (order.payment) {
        PaymentChoice.CardOnDelivery -> DesignR.drawable.ic_credit_card
        is PaymentChoice.Cash, PaymentChoice.PixOnDelivery -> DesignR.drawable.ic_banknote
    }
    InfoRow(paymentIcon, order.payment.label())
    order.notes?.let { InfoRow(DesignR.drawable.ic_receipt_text, stringResource(R.string.order_notes_line, it)) }
}

@Composable
private fun InfoRow(@DrawableRes icon: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PetjaTheme.spacing.s),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = PetjaTheme.colors.highlight,
            modifier = Modifier.size(PetjaTheme.sizes.icon),
        )
        Text(text = text, style = PetjaTheme.typography.body, color = PetjaTheme.colors.onSurfaceVariant)
    }
}

@Composable
private fun ValueRow(label: String, value: String, emphasis: Boolean = false) {
    val style = if (emphasis) PetjaTheme.typography.sectionTitle else PetjaTheme.typography.body
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = style, modifier = Modifier.weight(1f))
        Text(text = value, style = if (emphasis) style else PetjaTheme.typography.price)
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
