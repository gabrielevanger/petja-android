package io.github.gabrielevanger.xodopet.feature.storefront

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoSpacing
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
fun StorefrontRoute(viewModel: StorefrontViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StorefrontScreen(state = state, onSelectPlace = viewModel::selectPlace, onRetry = viewModel::retry)
}

@Composable
fun StorefrontScreen(state: StorefrontUiState, onSelectPlace: (SamplePlace) -> Unit, onRetry: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            PlacePicker(state, onSelectPlace)
            when (val content = state.content) {
                StorefrontContent.Loading -> Loading()
                StorefrontContent.Error -> Error(onRetry)
                is StorefrontContent.Loaded -> StoreList(content)
            }
        }
    }
}

@Composable
private fun PlacePicker(state: StorefrontUiState, onSelectPlace: (SamplePlace) -> Unit) {
    Column(modifier = Modifier.padding(top = XodoSpacing.md)) {
        Text(
            text = stringResource(R.string.storefront_deliver_to),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = XodoSpacing.md),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = XodoSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(XodoSpacing.sm),
        ) {
            items(state.places, key = { it.name }) { place ->
                FilterChip(
                    selected = place == state.selectedPlace,
                    onClick = { onSelectPlace(place) },
                    label = { Text(place.name) },
                )
            }
        }
        Text(
            text = stringResource(R.string.storefront_sample_places_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = XodoSpacing.md),
        )
    }
}

@Composable
private fun Loading() {
    Column(
        modifier = Modifier.fillMaxSize().padding(XodoSpacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.storefront_loading),
            modifier = Modifier.padding(top = XodoSpacing.md),
        )
    }
}

@Composable
private fun Error(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(XodoSpacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = stringResource(R.string.storefront_error), style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onRetry, modifier = Modifier.padding(top = XodoSpacing.md)) {
            Text(stringResource(R.string.storefront_retry))
        }
    }
}

@Composable
private fun StoreList(content: StorefrontContent.Loaded) {
    LazyColumn(
        contentPadding = PaddingValues(XodoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(XodoSpacing.sm),
    ) {
        if (content.noPartnerDelivers) {
            item(key = "no-partner") { SectionTitle(stringResource(R.string.storefront_no_partner_title)) }
        } else {
            item(key = "partners-title") { SectionTitle(stringResource(R.string.storefront_partners_title)) }
            items(content.partners, key = { it.id }, contentType = { "partner" }) { StoreCard(it) }
        }

        val showNonPartners = content.noPartnerDelivers || content.nonPartners.isNotEmpty()
        if (showNonPartners) {
            item(key = "non-partners-title") {
                Column(modifier = Modifier.padding(top = XodoSpacing.md)) {
                    if (!content.noPartnerDelivers) {
                        Text(
                            text = stringResource(R.string.storefront_all_closed_title),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = XodoSpacing.sm),
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
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(content.nonPartners, key = { it.id }, contentType = { "non-partner" }) { ExternalStoreCard(it) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun StoreCard(store: StoreCardUi) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(XodoSpacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = store.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                OpenBadge(store.isOpen)
            }
            Text(
                text = "${store.neighborhood} · ${store.distance}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(XodoSpacing.md),
                modifier = Modifier.padding(top = XodoSpacing.sm),
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

/** Texto e cor juntos: aberta ou fechada não depende só da cor (RNF-07). */
@Composable
private fun OpenBadge(isOpen: Boolean) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = if (isOpen) colors.secondaryContainer else colors.surfaceVariant,
        contentColor = if (isOpen) colors.onSecondaryContainer else colors.onSurfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = stringResource(if (isOpen) R.string.storefront_open else R.string.storefront_closed),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = XodoSpacing.sm, vertical = XodoSpacing.xs),
        )
    }
}

@Composable
private fun Detail(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun ExternalStoreCard(store: ExternalStoreCardUi) {
    val uriHandler = LocalUriHandler.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(XodoSpacing.md)) {
            Text(text = store.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = listOfNotNull(store.address, store.distance).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(XodoSpacing.sm),
                modifier = Modifier.padding(top = XodoSpacing.sm),
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

private val PreviewPlaces = BoaVistaSamplePlaces.toImmutableList()

@Preview(showBackground = true)
@Composable
private fun StorefrontPreview() {
    XodoTheme {
        StorefrontScreen(
            state = StorefrontUiState(
                places = PreviewPlaces,
                selectedPlace = PreviewPlaces.first(),
                content = StorefrontContent.Loaded(
                    partners = persistentListOf(
                        StoreCardUi("1", "Petshop Lavrado", "Centro", true, 40, "R$ 5,00", false, "R$ 30,00", "440 m"),
                        StoreCardUi("2", "Bicho Feliz", "Santa Tereza", false, 40, "R$ 6,50", false, null, "3,6 km"),
                    ),
                ),
            ),
            onSelectPlace = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoPartnerPreview() {
    XodoTheme {
        Box {
            StorefrontScreen(
                state = StorefrontUiState(
                    places = PreviewPlaces,
                    selectedPlace = PreviewPlaces.last(),
                    content = StorefrontContent.Loaded(
                        nonPartners = persistentListOf(
                            ExternalStoreCardUi(
                                "e1",
                                "Agropet Satélite",
                                "Rua Fictícia A, 10 · Cidade Satélite",
                                "310 m",
                                "https://wa.me/5595900001001",
                                "tel:9530001001",
                            ),
                        ),
                    ),
                ),
                onSelectPlace = {},
                onRetry = {},
            )
        }
    }
}
