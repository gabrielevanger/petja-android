package io.github.gabrielevanger.petja.feature.storefront

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gabrielevanger.petja.core.designsystem.component.BadgeTone
import io.github.gabrielevanger.petja.core.designsystem.component.ErrorState
import io.github.gabrielevanger.petja.core.designsystem.component.LoadingState
import io.github.gabrielevanger.petja.core.designsystem.component.SectionTitle
import io.github.gabrielevanger.petja.core.designsystem.component.StatusBadge
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme
import io.github.gabrielevanger.petja.core.ui.CartBar
import io.github.gabrielevanger.petja.core.ui.CollectEvents
import io.github.gabrielevanger.petja.core.ui.R as CoreUiR
import io.github.gabrielevanger.petja.core.ui.dotSeparated

@Composable
fun StorefrontRoute(
    onStoreClick: (storeId: String) -> Unit,
    onCartClick: () -> Unit,
    onAccountClick: () -> Unit,
    viewModel: StorefrontViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onAction(StorefrontAction.OnStart) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onAction(StorefrontAction.OnStop) }
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is StorefrontEvent.OpenStore -> onStoreClick(event.storeId)
            StorefrontEvent.OpenCart -> onCartClick()
            StorefrontEvent.OpenAccount -> onAccountClick()
        }
    }
    StorefrontScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun StorefrontScreen(state: StorefrontUiState, onAction: (StorefrontAction) -> Unit) {
    Scaffold(
        containerColor = PetjaTheme.colors.background,
        bottomBar = { state.cartBar?.let { CartBar(it, onClick = { onAction(StorefrontAction.OnCartClick) }) } },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            PlacePicker(state, onAction)
            when (val content = state.content) {
                StorefrontContent.Loading -> LoadingState(stringResource(R.string.storefront_loading))

                StorefrontContent.Error -> ErrorState(
                    message = stringResource(R.string.storefront_error),
                    actionLabel = stringResource(CoreUiR.string.common_retry),
                    onAction = { onAction(StorefrontAction.OnRetryClick) },
                )

                is StorefrontContent.Loaded -> StoreList(content) { onAction(StorefrontAction.OnStoreClick(it)) }
            }
        }
    }
}

@Composable
private fun PlacePicker(state: StorefrontUiState, onAction: (StorefrontAction) -> Unit) {
    val spacing = PetjaTheme.spacing
    Column(modifier = Modifier.padding(top = spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = spacing.m)) {
            Text(
                text = stringResource(R.string.storefront_deliver_to),
                style = PetjaTheme.typography.label,
                color = PetjaTheme.colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onAction(StorefrontAction.OnAccountClick) }) {
                Icon(Icons.Filled.AccountCircle, contentDescription = stringResource(R.string.storefront_account))
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = spacing.m),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            items(state.places, key = { it.name }) { place ->
                FilterChip(
                    selected = place == state.selectedPlace,
                    onClick = { onAction(StorefrontAction.OnPlaceClick(place)) },
                    label = { Text(place.name) },
                )
            }
        }
        Text(
            text = stringResource(R.string.storefront_sample_places_hint),
            style = PetjaTheme.typography.caption,
            color = PetjaTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = spacing.m),
        )
    }
}

@Composable
private fun StoreList(content: StorefrontContent.Loaded, onStoreClick: (storeId: String) -> Unit) {
    val spacing = PetjaTheme.spacing
    LazyColumn(
        contentPadding = PaddingValues(spacing.m),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        if (content.noPartnerDelivers) {
            item(key = "no-partner") { SectionTitle(stringResource(R.string.storefront_no_partner_title)) }
        } else {
            item(key = "partners-title") { SectionTitle(stringResource(R.string.storefront_partners_title)) }
            items(content.partners, key = { it.id }, contentType = { "partner" }) { store ->
                StoreCard(store, onClick = { onStoreClick(store.id) })
            }
        }

        if (content.noPartnerDelivers || content.nonPartners.isNotEmpty()) {
            item(key = "non-partners-title") { NonPartnersHeader(content) }
            items(content.nonPartners, key = { it.id }, contentType = { "non-partner" }) { ExternalStoreCard(it) }
        }
    }
}

@Composable
private fun NonPartnersHeader(content: StorefrontContent.Loaded) {
    Column(modifier = Modifier.padding(top = PetjaTheme.spacing.m)) {
        if (!content.noPartnerDelivers) {
            Text(
                text = stringResource(R.string.storefront_all_closed_title),
                style = PetjaTheme.typography.body,
                modifier = Modifier.padding(bottom = PetjaTheme.spacing.xs),
            )
        }
        SectionTitle(stringResource(R.string.storefront_non_partners_title))
        Text(
            text = stringResource(
                if (content.nonPartners.isEmpty()) {
                    R.string.storefront_non_partners_empty
                } else {
                    R.string.storefront_non_partners_note
                },
            ),
            style = PetjaTheme.typography.caption,
            color = PetjaTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun StoreCard(store: StoreCardUi, onClick: () -> Unit) {
    // Loja fechada também abre: o cliente pode ver o catálogo, só não pode pedir agora.
    Card(onClick = onClick, shape = PetjaTheme.shapes.card, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(PetjaTheme.spacing.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = store.name, style = PetjaTheme.typography.cardTitle, modifier = Modifier.weight(1f))
                StatusBadge(
                    text = stringResource(
                        if (store.isOpen) CoreUiR.string.store_status_open else CoreUiR.string.store_status_closed,
                    ),
                    tone = if (store.isOpen) BadgeTone.Positive else BadgeTone.Neutral,
                )
            }
            Text(
                text = dotSeparated(store.neighborhood, store.distance),
                style = PetjaTheme.typography.body,
                color = PetjaTheme.colors.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(PetjaTheme.spacing.m),
                modifier = Modifier.padding(top = PetjaTheme.spacing.xs),
            ) {
                Detail(stringResource(R.string.storefront_eta, store.estimatedMinutes))
                Detail(
                    if (store.isFreeDelivery) {
                        stringResource(R.string.storefront_free_delivery)
                    } else {
                        stringResource(R.string.storefront_delivery_fee, store.deliveryFee)
                    },
                )
                store.minOrder?.let { Detail(stringResource(R.string.storefront_min_order, it)) }
            }
        }
    }
}

@Composable
private fun Detail(text: String) {
    Text(text = text, style = PetjaTheme.typography.caption)
}

@Composable
private fun ExternalStoreCard(store: ExternalStoreCardUi) {
    val uriHandler = LocalUriHandler.current
    Card(shape = PetjaTheme.shapes.card, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(PetjaTheme.spacing.m)) {
            Text(text = store.name, style = PetjaTheme.typography.cardTitle)
            Text(
                text = dotSeparated(store.street, store.neighborhood, store.distance),
                style = PetjaTheme.typography.body,
                color = PetjaTheme.colors.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(PetjaTheme.spacing.xs),
                modifier = Modifier.padding(top = PetjaTheme.spacing.xs),
            ) {
                store.whatsappLink?.let { link ->
                    Button(onClick = { uriHandler.openUri(link) }) {
                        Text(stringResource(R.string.storefront_whatsapp))
                    }
                }
                store.phoneLink?.let { link ->
                    OutlinedButton(onClick = { uriHandler.openUri(link) }) {
                        Text(stringResource(R.string.storefront_call))
                    }
                }
            }
        }
    }
}
