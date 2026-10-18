package io.github.gabrielevanger.xodopet.feature.store

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoSpacing
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme
import io.github.gabrielevanger.xodopet.core.domain.model.PetSpecies
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@Composable
fun StoreRoute(onBack: () -> Unit, viewModel: StoreViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StoreScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onProductClick = viewModel::openProduct,
        onProductDismiss = viewModel::closeProduct,
    )
}

@Composable
fun StoreScreen(
    state: StoreUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onProductClick: (productId: String) -> Unit,
    onProductDismiss: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.safeDrawingPadding()) {
            TopBar(title = (state as? StoreUiState.Loaded)?.header?.name, onBack = onBack)
            when (state) {
                StoreUiState.Loading -> Centered {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.store_loading), modifier = Modifier.padding(top = XodoSpacing.md))
                }

                StoreUiState.Error -> Centered {
                    Text(stringResource(R.string.store_error), style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = onRetry, modifier = Modifier.padding(top = XodoSpacing.md)) {
                        Text(stringResource(R.string.store_retry))
                    }
                }

                StoreUiState.NotFound -> Centered {
                    Text(stringResource(R.string.store_not_found), style = MaterialTheme.typography.bodyLarge)
                }

                is StoreUiState.Loaded -> {
                    Catalog(state, onProductClick)
                    state.selectedProduct?.let { ProductSheet(it, onDismiss = onProductDismiss) }
                }
            }
        }
    }
}

@Composable
private fun TopBar(title: String?, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(XodoSpacing.xs)) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.store_back))
        }
        if (title != null) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, maxLines = 1)
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(XodoSpacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

@Composable
private fun Catalog(state: StoreUiState.Loaded, onProductClick: (String) -> Unit) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Posição de cada cabeçalho de categoria na lista: 1 item do cabeçalho da loja, depois título + produtos.
    val sectionIndexes = state.sections.runningFold(1) { index, section -> index + 1 + section.products.size }

    Column {
        if (state.sections.size > 1) {
            CategoryChips(state.sections) { position ->
                scope.launch { listState.animateScrollToItem(sectionIndexes[position]) }
            }
        }
        CatalogList(state, listState, onProductClick)
    }
}

@Composable
private fun CategoryChips(sections: List<CatalogSectionUi>, onClick: (position: Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = XodoSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(XodoSpacing.sm),
    ) {
        items(sections.size, key = { sections[it].categoryId }) { position ->
            SuggestionChip(onClick = { onClick(position) }, label = { Text(sections[position].title) })
        }
    }
}

@Composable
private fun CatalogList(state: StoreUiState.Loaded, listState: LazyListState, onProductClick: (String) -> Unit) {
    LazyColumn(state = listState, contentPadding = PaddingValues(bottom = XodoSpacing.xl)) {
        item(key = "header") { StoreHeader(state.header) }
        if (state.sections.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.store_empty_catalog),
                    modifier = Modifier.padding(XodoSpacing.md),
                )
            }
        }
        state.sections.forEach { section ->
            item(key = "section-${section.categoryId}", contentType = "section") {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(
                            start = XodoSpacing.md,
                            end = XodoSpacing.md,
                            top = XodoSpacing.lg,
                            bottom = XodoSpacing.xs,
                        )
                        .semantics { heading() },
                )
            }
            items(section.products, key = { it.id }, contentType = { "product" }) { product ->
                ProductRow(product, onClick = { onProductClick(product.id) })
            }
        }
    }
}

@Composable
private fun StoreHeader(header: StoreHeaderUi) {
    Column(modifier = Modifier.padding(horizontal = XodoSpacing.md, vertical = XodoSpacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = header.neighborhood,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            OpenBadge(header.isOpen)
        }
        header.description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = XodoSpacing.xs),
            )
        }
        Text(
            text = listOfNotNull(
                stringResource(R.string.store_eta, header.estimatedMinutes),
                header.minOrder?.let { stringResource(R.string.store_min_order, it) },
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = XodoSpacing.xs),
        )
        if (!header.isOpen) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().padding(top = XodoSpacing.sm),
            ) {
                Text(
                    text = stringResource(R.string.store_closed_notice),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(XodoSpacing.md),
                )
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
            text = stringResource(if (isOpen) R.string.store_open else R.string.store_closed),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = XodoSpacing.sm, vertical = XodoSpacing.xs),
        )
    }
}

@Composable
private fun ProductRow(product: ProductRowUi, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = XodoSpacing.md, vertical = XodoSpacing.sm),
    ) {
        val contentColor = if (product.isAvailable) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
        Text(text = product.name, style = MaterialTheme.typography.bodyLarge, color = contentColor)
        Row {
            Text(
                text = product.brand.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = when {
                    product.price == null -> stringResource(R.string.store_unavailable)
                    product.fromPrice -> stringResource(R.string.store_price_from, product.price)
                    else -> product.price
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (product.isAvailable) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor,
            )
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = XodoSpacing.md))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductSheet(product: ProductDetailUi, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(start = XodoSpacing.md, end = XodoSpacing.md, bottom = XodoSpacing.xl)) {
            Text(text = product.name, style = MaterialTheme.typography.titleLarge)
            val subtitle =
                listOfNotNull(product.brand, product.species.map { speciesLabel(it) }.joinToString().ifBlank { null })
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            product.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = XodoSpacing.sm),
                )
            }
            Text(
                text = stringResource(R.string.store_options_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = XodoSpacing.md, bottom = XodoSpacing.xs).semantics { heading() },
            )
            product.variants.forEach { variant ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = XodoSpacing.xs)) {
                    Text(text = variant.label, modifier = Modifier.weight(1f))
                    Text(
                        text = if (variant.isAvailable) variant.price else stringResource(R.string.store_unavailable),
                        fontWeight = if (variant.isAvailable) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (variant.isAvailable) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            Text(
                text = stringResource(R.string.store_cart_soon),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = XodoSpacing.md),
            )
        }
    }
}

@Composable
private fun speciesLabel(species: PetSpecies): String = stringResource(
    when (species) {
        PetSpecies.DOG -> R.string.species_dog
        PetSpecies.CAT -> R.string.species_cat
        PetSpecies.BIRD -> R.string.species_bird
        PetSpecies.FISH -> R.string.species_fish
        PetSpecies.RODENT -> R.string.species_rodent
        PetSpecies.REPTILE -> R.string.species_reptile
        PetSpecies.OTHER -> R.string.species_other
    },
)

@Preview(showBackground = true)
@Composable
private fun StorePreview() {
    XodoTheme {
        StoreScreen(
            state = StoreUiState.Loaded(
                header = StoreHeaderUi(
                    "Petshop Lavrado",
                    "Centro",
                    "Ração, petiscos e acessórios.",
                    false,
                    40,
                    "R$ 30,00",
                ),
                sections = persistentListOf(
                    CatalogSectionUi(
                        1,
                        "Ração",
                        persistentListOf(
                            ProductRowUi("1", "Ração Golden Adultos", "Golden", "R$ 30,90", true, true),
                            ProductRowUi("2", "Ração Pedigree Adultos", "Pedigree", null, false, false),
                        ),
                    ),
                ),
            ),
            onBack = {},
            onRetry = {},
            onProductClick = {},
            onProductDismiss = {},
        )
    }
}
