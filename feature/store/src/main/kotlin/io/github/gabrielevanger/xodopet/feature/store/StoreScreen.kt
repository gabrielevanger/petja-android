package io.github.gabrielevanger.xodopet.feature.store

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gabrielevanger.xodopet.core.designsystem.component.BadgeTone
import io.github.gabrielevanger.xodopet.core.designsystem.component.ErrorState
import io.github.gabrielevanger.xodopet.core.designsystem.component.LoadingState
import io.github.gabrielevanger.xodopet.core.designsystem.component.MessageState
import io.github.gabrielevanger.xodopet.core.designsystem.component.Notice
import io.github.gabrielevanger.xodopet.core.designsystem.component.SectionTitle
import io.github.gabrielevanger.xodopet.core.designsystem.component.StatusBadge
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme
import io.github.gabrielevanger.xodopet.core.domain.model.PetSpecies
import io.github.gabrielevanger.xodopet.core.ui.CartBar
import io.github.gabrielevanger.xodopet.core.ui.CollectEvents
import io.github.gabrielevanger.xodopet.core.ui.R as CoreUiR
import io.github.gabrielevanger.xodopet.core.ui.dotSeparated
import kotlinx.coroutines.launch

/** Nome da loja numa linha só na barra do topo: nomes longos terminam em reticências. */
private const val TOP_BAR_TITLE_MAX_LINES = 1

/** Antes da primeira seção da lista vem só o cabeçalho da loja. */
private const val ITEMS_BEFORE_FIRST_SECTION = 1

@Composable
fun StoreRoute(onBack: () -> Unit, onCartClick: () -> Unit, viewModel: StoreViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // O aviso chega como evento e fica aqui até o snackbar terminar de mostrar.
    var message by remember { mutableStateOf<StoreCartMessage?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onAction(StoreAction.OnStart) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onAction(StoreAction.OnStop) }
    CollectEvents(viewModel.events) { event ->
        when (event) {
            StoreEvent.Close -> onBack()
            StoreEvent.OpenCart -> onCartClick()
            is StoreEvent.ShowMessage -> message = event.message
        }
    }
    StoreScreen(state, viewModel::onAction, message = message, onMessageShown = { message = null })
}

@Composable
fun StoreScreen(
    state: StoreUiState,
    onAction: (StoreAction) -> Unit,
    message: StoreCartMessage? = null,
    onMessageShown: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    message?.let { message ->
        val text = message.text()
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(text)
            onMessageShown()
        }
    }
    state.replacePrompt?.let {
        ReplaceCartDialog(
            it,
            onConfirm = { onAction(StoreAction.OnConfirmReplaceCart) },
            onDismiss = { onAction(StoreAction.OnDismissReplaceCart) },
        )
    }

    Scaffold(
        containerColor = XodoTheme.colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { state.cartBar?.let { CartBar(it, onClick = { onAction(StoreAction.OnCartClick) }) } },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            val content = state.content
            TopBar(title = (content as? StoreContent.Loaded)?.header?.name, onBack = {
                onAction(StoreAction.OnBackClick)
            })
            when (content) {
                StoreContent.Loading -> LoadingState(stringResource(R.string.store_loading))

                StoreContent.Error -> ErrorState(
                    message = stringResource(R.string.store_error),
                    actionLabel = stringResource(CoreUiR.string.common_retry),
                    onAction = { onAction(StoreAction.OnRetryClick) },
                )

                StoreContent.NotFound -> MessageState(stringResource(R.string.store_not_found))

                is StoreContent.Loaded -> {
                    Catalog(content) { onAction(StoreAction.OnProductClick(it)) }
                    content.selectedProduct?.let {
                        ProductSheet(
                            it,
                            onAdd = { variantId -> onAction(StoreAction.OnAddToCart(variantId)) },
                            onDismiss = { onAction(StoreAction.OnProductDismiss) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(title: String?, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(XodoTheme.spacing.xxs)) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(CoreUiR.string.common_back))
        }
        if (title != null) {
            Text(
                text = title,
                style = XodoTheme.typography.screenTitle,
                maxLines = TOP_BAR_TITLE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun Catalog(state: StoreContent.Loaded, onProductClick: (String) -> Unit) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Posição do título de cada seção na lista: depois do cabeçalho da loja, título + produtos de cada seção.
    val sectionIndexes = state.sections.runningFold(ITEMS_BEFORE_FIRST_SECTION) { index, section ->
        index + 1 + section.products.size
    }

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
        contentPadding = PaddingValues(horizontal = XodoTheme.spacing.m),
        horizontalArrangement = Arrangement.spacedBy(XodoTheme.spacing.xs),
    ) {
        items(sections.size, key = { sections[it].categoryId }) { position ->
            SuggestionChip(onClick = { onClick(position) }, label = { Text(sections[position].title) })
        }
    }
}

@Composable
private fun CatalogList(state: StoreContent.Loaded, listState: LazyListState, onProductClick: (String) -> Unit) {
    val spacing = XodoTheme.spacing
    LazyColumn(state = listState, contentPadding = PaddingValues(bottom = spacing.xl)) {
        item(key = "header") { StoreHeader(state.header) }
        if (state.sections.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.store_empty_catalog),
                    style = XodoTheme.typography.body,
                    modifier = Modifier.padding(spacing.m),
                )
            }
        }
        state.sections.forEach { section ->
            item(key = "section-${section.categoryId}", contentType = "section") {
                SectionTitle(
                    text = section.title,
                    modifier = Modifier.padding(
                        start = spacing.m,
                        end = spacing.m,
                        top = spacing.l,
                        bottom = spacing.xxs,
                    ),
                )
            }
            items(section.products.size, key = { section.products[it].id }, contentType = { "product" }) { index ->
                val product = section.products[index]
                ProductRow(product, onClick = { onProductClick(product.id) })
            }
        }
    }
}

@Composable
private fun StoreHeader(header: StoreHeaderUi) {
    val spacing = XodoTheme.spacing
    Column(modifier = Modifier.padding(horizontal = spacing.m, vertical = spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = header.neighborhood,
                style = XodoTheme.typography.body,
                color = XodoTheme.colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            StatusBadge(
                text = stringResource(
                    if (header.isOpen) CoreUiR.string.store_status_open else CoreUiR.string.store_status_closed,
                ),
                tone = if (header.isOpen) BadgeTone.Positive else BadgeTone.Neutral,
            )
        }
        header.description?.let {
            Text(text = it, style = XodoTheme.typography.body, modifier = Modifier.padding(top = spacing.xxs))
        }
        Text(
            text = dotSeparated(
                stringResource(R.string.store_eta, header.estimatedMinutes),
                header.minOrder?.let { stringResource(R.string.store_min_order, it) },
            ),
            style = XodoTheme.typography.caption,
            modifier = Modifier.padding(top = spacing.xxs),
        )
        if (!header.isOpen) {
            Notice(stringResource(R.string.store_closed_notice), modifier = Modifier.padding(top = spacing.xs))
        }
    }
}

@Composable
private fun ProductRow(product: ProductRowUi, onClick: () -> Unit) {
    val spacing = XodoTheme.spacing
    val contentColor = if (product.isAvailable) XodoTheme.colors.onSurface else XodoTheme.colors.onSurfaceVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.m, vertical = spacing.xs),
    ) {
        Text(text = product.name, style = XodoTheme.typography.itemName, color = contentColor)
        Row {
            Text(
                text = product.brand.orEmpty(),
                style = XodoTheme.typography.caption,
                color = XodoTheme.colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = when {
                    product.price == null -> stringResource(R.string.store_unavailable)
                    product.fromPrice -> stringResource(R.string.store_price_from, product.price)
                    else -> product.price
                },
                style = if (product.isAvailable) XodoTheme.typography.price else XodoTheme.typography.body,
                color = contentColor,
            )
        }
    }
    HorizontalDivider(thickness = XodoTheme.sizes.divider, modifier = Modifier.padding(horizontal = spacing.m))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductSheet(product: ProductDetailUi, onAdd: (variantId: String) -> Unit, onDismiss: () -> Unit) {
    val spacing = XodoTheme.spacing
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(start = spacing.m, end = spacing.m, bottom = spacing.xl)) {
            Text(text = product.name, style = XodoTheme.typography.screenTitle)
            val speciesText = dotSeparated(*product.species.map { speciesLabel(it) }.toTypedArray())
            val subtitle = dotSeparated(product.brand, speciesText)
            if (subtitle.isNotEmpty()) {
                Text(text = subtitle, style = XodoTheme.typography.body, color = XodoTheme.colors.onSurfaceVariant)
            }
            product.description?.let {
                Text(text = it, style = XodoTheme.typography.body, modifier = Modifier.padding(top = spacing.xs))
            }
            SectionTitle(
                text = stringResource(R.string.store_options_title),
                modifier = Modifier.padding(top = spacing.m, bottom = spacing.xxs),
            )
            product.variants.forEach { variant -> VariantRow(variant, onAdd = { onAdd(variant.id) }) }
        }
    }
}

@Composable
private fun VariantRow(variant: VariantUi, onAdd: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(XodoTheme.spacing.s),
        modifier = Modifier.fillMaxWidth().padding(vertical = XodoTheme.spacing.xxs),
    ) {
        Text(text = variant.label, style = XodoTheme.typography.body, modifier = Modifier.weight(1f))
        if (variant.isAvailable) {
            Text(text = variant.price, style = XodoTheme.typography.price)
            FilledTonalButton(onClick = onAdd) { Text(stringResource(R.string.store_add)) }
        } else {
            Text(
                text = stringResource(R.string.store_unavailable),
                style = XodoTheme.typography.body,
                color = XodoTheme.colors.onSurfaceVariant,
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

@Composable
private fun ReplaceCartDialog(prompt: ReplaceCartPrompt, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.store_replace_title)) },
        text = { Text(stringResource(R.string.store_replace_text, prompt.currentStoreName)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.store_replace_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.store_replace_dismiss)) } },
    )
}

@Composable
private fun StoreCartMessage.text(): String = when (this) {
    is StoreCartMessage.Added -> stringResource(R.string.store_added, productName, variantLabel)
    StoreCartMessage.LimitReached -> stringResource(R.string.store_limit_reached)
    StoreCartMessage.Unavailable -> stringResource(R.string.store_option_unavailable)
}
