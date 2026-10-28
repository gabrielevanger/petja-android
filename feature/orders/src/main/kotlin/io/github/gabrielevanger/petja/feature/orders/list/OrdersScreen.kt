package io.github.gabrielevanger.petja.feature.orders.list

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
fun OrdersRoute(
    onBack: () -> Unit,
    onOpenOrder: (String) -> Unit,
    onSignIn: () -> Unit,
    viewModel: OrdersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Toda vez que a lista aparece (inclusive ao voltar dos detalhes), os status se atualizam.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onAction(OrdersAction.OnStart) }
    CollectEvents(viewModel.events) { event ->
        when (event) {
            OrdersEvent.Close -> onBack()
            is OrdersEvent.OpenOrder -> onOpenOrder(event.orderId)
            OrdersEvent.OpenSignIn -> onSignIn()
        }
    }
    OrdersScreen(state, viewModel::onAction)
}

@Composable
fun OrdersScreen(state: OrdersUiState, onAction: (OrdersAction) -> Unit) {
    Scaffold(containerColor = PetjaTheme.colors.background, topBar = {
        BackTopBar(stringResource(R.string.orders_title), onBack = { onAction(OrdersAction.OnBackClick) })
    }) { padding ->
        val modifier = Modifier.padding(padding).consumeWindowInsets(padding)
        when (state) {
            OrdersUiState.Loading -> LoadingState(stringResource(R.string.orders_loading), modifier)

            OrdersUiState.Error -> ErrorState(
                stringResource(R.string.orders_error),
                stringResource(CoreUiR.string.common_retry),
                { onAction(OrdersAction.OnRetryClick) },
                modifier,
            )

            OrdersUiState.NotSignedIn -> ErrorState(
                stringResource(R.string.orders_not_signed_in),
                stringResource(R.string.order_sign_in),
                { onAction(OrdersAction.OnSignInClick) },
                modifier,
            )

            is OrdersUiState.Loaded -> if (state.orders.isEmpty()) {
                MessageState(stringResource(R.string.orders_empty), modifier)
            } else {
                OrdersList(state, { onAction(OrdersAction.OnOrderClick(it)) }, modifier)
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
