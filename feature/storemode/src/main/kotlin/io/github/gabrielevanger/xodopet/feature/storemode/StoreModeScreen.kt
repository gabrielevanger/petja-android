package io.github.gabrielevanger.xodopet.feature.storemode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gabrielevanger.xodopet.core.designsystem.component.BadgeTone
import io.github.gabrielevanger.xodopet.core.designsystem.component.ErrorState
import io.github.gabrielevanger.xodopet.core.designsystem.component.FormField
import io.github.gabrielevanger.xodopet.core.designsystem.component.LoadingState
import io.github.gabrielevanger.xodopet.core.designsystem.component.MessageState
import io.github.gabrielevanger.xodopet.core.designsystem.component.SectionTitle
import io.github.gabrielevanger.xodopet.core.designsystem.component.StatusBadge
import io.github.gabrielevanger.xodopet.core.designsystem.component.XodoButton
import io.github.gabrielevanger.xodopet.core.designsystem.component.XodoButtonStyle
import io.github.gabrielevanger.xodopet.core.designsystem.component.XodoCard
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme
import io.github.gabrielevanger.xodopet.core.designsystem.theme.highlightContainer
import io.github.gabrielevanger.xodopet.core.domain.checkout.PaymentChoice
import io.github.gabrielevanger.xodopet.core.domain.model.OrderStatus
import io.github.gabrielevanger.xodopet.core.domain.storemode.StoreAction
import io.github.gabrielevanger.xodopet.core.domain.storemode.StoreOrder
import io.github.gabrielevanger.xodopet.core.domain.storemode.actions
import io.github.gabrielevanger.xodopet.core.ui.BackTopBar
import io.github.gabrielevanger.xodopet.core.ui.R as CoreUiR
import io.github.gabrielevanger.xodopet.core.ui.dotSeparated
import io.github.gabrielevanger.xodopet.core.ui.formatBrl
import io.github.gabrielevanger.xodopet.core.ui.formatDateTime

@Composable
fun StoreModeRoute(onBack: () -> Unit, onSignIn: () -> Unit, viewModel: StoreModeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Como num KDS, a tela não apaga enquanto a loja está nela.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    StoreModeScreen(
        state,
        StoreModeActions(
            onBack = onBack,
            onSignIn = onSignIn,
            onRetry = viewModel::retry,
            onSelectStore = viewModel::selectStore,
            onAct = viewModel::act,
            onConfirmReason = viewModel::confirmReason,
            onDismissReason = viewModel::dismissReason,
            onMessageShown = viewModel::messageShown,
        ),
    )
}

class StoreModeActions(
    val onBack: () -> Unit = {},
    val onSignIn: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onSelectStore: (String) -> Unit = {},
    val onAct: (StoreOrder, StoreAction) -> Unit = { _, _ -> },
    val onConfirmReason: (String) -> Unit = {},
    val onDismissReason: () -> Unit = {},
    val onMessageShown: () -> Unit = {},
)

@Composable
fun StoreModeScreen(state: StoreModeUiState, actions: StoreModeActions) {
    val snackbar = remember { SnackbarHostState() }
    val loaded = state as? StoreModeUiState.Loaded
    loaded?.message?.let { message ->
        val text = message.text()
        LaunchedEffect(message) {
            snackbar.showSnackbar(text)
            actions.onMessageShown()
        }
    }
    loaded?.askingReason?.let { ReasonDialog(it, actions) }
    Scaffold(
        containerColor = XodoTheme.colors.background,
        topBar = { BackTopBar(stringResource(R.string.store_mode_title), actions.onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val modifier = Modifier.padding(padding).consumeWindowInsets(padding)
        when (state) {
            StoreModeUiState.Loading -> LoadingState(stringResource(R.string.store_mode_loading), modifier)

            StoreModeUiState.Error -> ErrorState(
                stringResource(R.string.store_mode_error),
                stringResource(CoreUiR.string.common_retry),
                actions.onRetry,
                modifier,
            )

            StoreModeUiState.NotSignedIn -> ErrorState(
                stringResource(R.string.store_mode_not_signed_in),
                stringResource(R.string.store_mode_sign_in),
                actions.onSignIn,
                modifier,
            )

            StoreModeUiState.NoStore -> MessageState(stringResource(R.string.store_mode_no_store), modifier)

            is StoreModeUiState.Loaded -> StoreOrders(state, actions, modifier)
        }
    }
}

@Composable
private fun StoreOrders(state: StoreModeUiState.Loaded, actions: StoreModeActions, modifier: Modifier) {
    val spacing = XodoTheme.spacing
    val pending = state.orders.filter { it.status == OrderStatus.Pending }
    val active = state.orders.filter { it.status != OrderStatus.Pending && !it.status.isFinal }
    val finished = state.orders.filter { it.status.isFinal }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(spacing.s),
        contentPadding = PaddingValues(horizontal = spacing.l, vertical = spacing.m),
        modifier = modifier,
    ) {
        item(key = "header") { Header(state, actions) }
        if (state.orders.isEmpty()) {
            item(key = "empty") { MessageState(stringResource(R.string.store_mode_empty)) }
        }
        section("new", R.string.store_mode_section_new, pending, state, actions)
        section("active", R.string.store_mode_section_active, active, state, actions)
        section("finished", R.string.store_mode_section_finished, finished, state, actions)
    }
}

private fun LazyListScope.section(
    key: String,
    title: Int,
    orders: List<StoreOrder>,
    state: StoreModeUiState.Loaded,
    actions: StoreModeActions,
) {
    if (orders.isEmpty()) return
    item(key = key) {
        SectionTitle(stringResource(title), modifier = Modifier.padding(top = XodoTheme.spacing.s))
    }
    items(orders, key = { it.id }) { order -> StoreOrderCard(order, working = state.working == order.id, actions) }
}

/** Nome da loja (ou a escolha, para quem opera mais de uma) e o aviso de que a tela se atualiza. */
@Composable
private fun Header(state: StoreModeUiState.Loaded, actions: StoreModeActions) {
    Column(verticalArrangement = Arrangement.spacedBy(XodoTheme.spacing.xs)) {
        if (state.stores.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(XodoTheme.spacing.xs)) {
                items(state.stores, key = { it.id }) { store ->
                    FilterChip(
                        selected = store.id == state.storeId,
                        onClick = { actions.onSelectStore(store.id) },
                        label = { Text(store.name) },
                    )
                }
            }
        } else {
            Text(text = state.storeName, style = XodoTheme.typography.headline)
        }
        Text(
            text = stringResource(R.string.store_mode_live),
            style = XodoTheme.typography.caption,
            color = XodoTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun StoreOrderCard(order: StoreOrder, working: Boolean, actions: StoreModeActions) {
    val spacing = XodoTheme.spacing
    val muted = XodoTheme.colors.onSurfaceVariant
    XodoCard(
        shape = XodoTheme.shapes.panel,
        color = if (order.status == OrderStatus.Pending) {
            XodoTheme.colors.highlightContainer
        } else {
            XodoTheme.colors.surfaceContainerLowest
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.store_order_title, order.number, order.customerName),
                    style = XodoTheme.typography.cardTitle,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(order.status.storeLabel(), order.status.storeTone)
            }
            Text(
                text = dotSeparated(formatDateTime(order.createdAt), order.total.formatBrl(), order.customerPhone),
                style = XodoTheme.typography.caption,
                color = muted,
            )
            order.items.forEach { item ->
                Text(
                    text = stringResource(
                        R.string.store_order_item,
                        item.quantity,
                        item.productName,
                        item.variantLabel,
                    ),
                    style = XodoTheme.typography.body,
                )
            }
            val address = order.address
            Text(
                text = dotSeparated(
                    stringResource(R.string.store_order_address, address.street, address.number),
                    address.complement,
                    address.neighborhood,
                    address.reference,
                ),
                style = XodoTheme.typography.body,
                color = muted,
            )
            Text(text = order.payment.storeLabel(), style = XodoTheme.typography.body, color = muted)
            order.notes?.let { Text(stringResource(R.string.store_order_notes, it), style = XodoTheme.typography.body) }
            order.statusReason?.let {
                Text(
                    stringResource(R.string.store_order_reason, it),
                    style = XodoTheme.typography.caption,
                    color = muted,
                )
            }
            order.actions.forEachIndexed { index, action ->
                XodoButton(
                    text = action.label(),
                    onClick = { actions.onAct(order, action) },
                    style = if (index == 0) XodoButtonStyle.Filled else XodoButtonStyle.Outlined,
                    enabled = !working,
                    loading = working && index == 0,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Recusa e não entrega pedem o motivo, que o cliente vê no acompanhamento. */
@Composable
private fun ReasonDialog(request: ReasonRequest, actions: StoreModeActions) {
    var reason by rememberSaveable(request) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = actions.onDismissReason,
        title = {
            Text(
                stringResource(
                    if (request.action == StoreAction.Reject) {
                        R.string.store_reason_reject_title
                    } else {
                        R.string.store_reason_not_deliver_title
                    },
                    request.number,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(XodoTheme.spacing.s)) {
                Text(stringResource(R.string.store_reason_text), style = XodoTheme.typography.body)
                FormField(value = reason, onValueChange = {
                    reason = it
                }, label = stringResource(R.string.store_reason_field))
            }
        },
        confirmButton = {
            TextButton(onClick = { actions.onConfirmReason(reason) }, enabled = reason.isNotBlank()) {
                Text(stringResource(R.string.store_reason_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onDismissReason) { Text(stringResource(R.string.store_reason_dismiss)) }
        },
    )
}

@Composable
private fun OrderStatus.storeLabel(): String = stringResource(
    when (this) {
        OrderStatus.Pending -> R.string.store_status_pending
        OrderStatus.Accepted -> R.string.store_status_accepted
        OrderStatus.Preparing -> R.string.store_status_preparing
        OrderStatus.OutForDelivery -> R.string.store_status_out_for_delivery
        OrderStatus.Delivered -> R.string.store_status_delivered
        OrderStatus.NotDelivered -> R.string.store_status_not_delivered
        OrderStatus.Rejected -> R.string.store_status_rejected
        OrderStatus.Canceled -> R.string.store_status_canceled
    },
)

/** O que não deu certo fica neutro; o resto, em destaque. */
private val OrderStatus.storeTone: BadgeTone
    get() = when (this) {
        OrderStatus.NotDelivered, OrderStatus.Rejected, OrderStatus.Canceled -> BadgeTone.Neutral

        OrderStatus.Pending, OrderStatus.Accepted, OrderStatus.Preparing, OrderStatus.OutForDelivery,
        OrderStatus.Delivered,
        -> BadgeTone.Positive
    }

@Composable
private fun StoreAction.label(): String = stringResource(
    when (this) {
        StoreAction.Accept -> R.string.store_action_accept
        StoreAction.Reject -> R.string.store_action_reject
        StoreAction.StartPreparing -> R.string.store_action_start_preparing
        StoreAction.SendOut -> R.string.store_action_send_out
        StoreAction.Deliver -> R.string.store_action_deliver
        StoreAction.NotDeliver -> R.string.store_action_not_deliver
    },
)

@Composable
private fun PaymentChoice.storeLabel(): String = when (this) {
    is PaymentChoice.Cash -> changeFor?.let { stringResource(R.string.store_order_payment_cash_change, it.formatBrl()) }
        ?: stringResource(R.string.store_order_payment_cash)

    PaymentChoice.PixOnDelivery -> stringResource(R.string.store_order_payment_pix)

    PaymentChoice.CardOnDelivery -> stringResource(R.string.store_order_payment_card)
}

@Composable
private fun StoreMessage.text(): String = stringResource(
    when (this) {
        StoreMessage.ActionFailed -> R.string.store_message_action_failed
        StoreMessage.OutOfDate -> R.string.store_message_out_of_date
        StoreMessage.NotAllowed -> R.string.store_message_not_allowed
    },
)
