package io.github.gabrielevanger.petja.feature.orders.details

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import io.github.gabrielevanger.petja.core.ui.CollectEvents
import io.github.gabrielevanger.petja.core.ui.R as CoreUiR
import io.github.gabrielevanger.petja.core.ui.dotSeparated
import io.github.gabrielevanger.petja.core.ui.formatBrl
import io.github.gabrielevanger.petja.core.ui.formatDateTime
import io.github.gabrielevanger.petja.core.ui.whatsappLink
import io.github.gabrielevanger.petja.feature.orders.R
import io.github.gabrielevanger.petja.feature.orders.headline
import io.github.gabrielevanger.petja.feature.orders.icon
import io.github.gabrielevanger.petja.feature.orders.label
import io.github.gabrielevanger.petja.feature.orders.tone

@Composable
fun OrderDetailsRoute(onBack: () -> Unit, onOpenCart: () -> Unit, viewModel: OrderDetailsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    // O aviso chega como evento e fica aqui até o snackbar terminar de mostrar.
    var message by remember { mutableStateOf<OrderMessage?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onAction(OrderDetailsAction.OnStart) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onAction(OrderDetailsAction.OnStop) }
    CollectEvents(viewModel.events) { event ->
        when (event) {
            OrderDetailsEvent.Close -> onBack()
            OrderDetailsEvent.OpenCart -> onOpenCart()
            is OrderDetailsEvent.OpenLink -> uriHandler.openUri(event.uri)
            is OrderDetailsEvent.ShowMessage -> message = event.message
        }
    }
    OrderDetailsScreen(state, viewModel::onAction, message = message, onMessageShown = { message = null })
}

@Composable
fun OrderDetailsScreen(
    state: OrderDetailsUiState,
    onAction: (OrderDetailsAction) -> Unit,
    message: OrderMessage? = null,
    onMessageShown: () -> Unit = {},
) {
    val snackbar = remember { SnackbarHostState() }
    val loaded = state as? OrderDetailsUiState.Loaded
    message?.let { shown ->
        val text = shown.text()
        LaunchedEffect(shown) {
            snackbar.showSnackbar(text)
            onMessageShown()
        }
    }
    if (loaded?.confirmingCancel == true) {
        AlertDialog(
            onDismissRequest = { onAction(OrderDetailsAction.OnDismissCancel) },
            title = { Text(stringResource(R.string.order_cancel_title)) },
            text = { Text(stringResource(R.string.order_cancel_text)) },
            confirmButton = {
                TextButton(onClick = { onAction(OrderDetailsAction.OnConfirmCancel) }) {
                    Text(stringResource(R.string.order_cancel_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(OrderDetailsAction.OnDismissCancel) }) {
                    Text(stringResource(R.string.order_cancel_dismiss))
                }
            },
        )
    }
    Scaffold(
        containerColor = PetjaTheme.colors.background,
        topBar = {
            BackTopBar(
                loaded?.let { stringResource(R.string.order_title, it.order.number) },
                onBack = { onAction(OrderDetailsAction.OnBackClick) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val modifier = Modifier.padding(padding).consumeWindowInsets(padding)
        when (state) {
            OrderDetailsUiState.Loading -> LoadingState(stringResource(R.string.order_loading), modifier)

            OrderDetailsUiState.Error -> ErrorState(
                stringResource(R.string.order_error),
                stringResource(CoreUiR.string.common_retry),
                { onAction(OrderDetailsAction.OnRetryClick) },
                modifier,
            )

            OrderDetailsUiState.NotFound -> MessageState(stringResource(R.string.order_not_found), modifier)

            is OrderDetailsUiState.Loaded -> Column(
                verticalArrangement = Arrangement.spacedBy(PetjaTheme.spacing.l),
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PetjaTheme.spacing.l, vertical = PetjaTheme.spacing.m),
            ) { OrderContent(state, onAction) }
        }
    }
}

@Composable
private fun OrderContent(state: OrderDetailsUiState.Loaded, onAction: (OrderDetailsAction) -> Unit) {
    val order = state.order
    StatusHeader(order)
    order.statusReason?.let { Notice(it) }
    Actions(state, onAction)
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
private fun Actions(state: OrderDetailsUiState.Loaded, onAction: (OrderDetailsAction) -> Unit) {
    val order = state.order
    Column(verticalArrangement = Arrangement.spacedBy(PetjaTheme.spacing.xs)) {
        PetjaButton(
            text = stringResource(R.string.order_reorder),
            onClick = { onAction(OrderDetailsAction.OnReorderClick) },
            enabled = !state.working,
            modifier = Modifier.fillMaxWidth(),
        )
        order.storeWhatsapp?.let { number ->
            PetjaButton(
                text = stringResource(R.string.order_whatsapp),
                onClick = { onAction(OrderDetailsAction.OnWhatsappClick(number)) },
                style = PetjaButtonStyle.Tonal,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (order.status.customerCanCancel) {
            PetjaButton(
                text = stringResource(R.string.order_cancel),
                onClick = { onAction(OrderDetailsAction.OnCancelClick) },
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
