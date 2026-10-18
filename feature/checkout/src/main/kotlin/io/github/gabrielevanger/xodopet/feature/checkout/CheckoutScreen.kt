package io.github.gabrielevanger.xodopet.feature.checkout

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gabrielevanger.xodopet.core.designsystem.R as DesignR
import io.github.gabrielevanger.xodopet.core.designsystem.component.BadgeTone
import io.github.gabrielevanger.xodopet.core.designsystem.component.FormField
import io.github.gabrielevanger.xodopet.core.designsystem.component.LoadingState
import io.github.gabrielevanger.xodopet.core.designsystem.component.MessageState
import io.github.gabrielevanger.xodopet.core.designsystem.component.Notice
import io.github.gabrielevanger.xodopet.core.designsystem.component.SectionTitle
import io.github.gabrielevanger.xodopet.core.designsystem.component.SelectableOption
import io.github.gabrielevanger.xodopet.core.designsystem.component.StatusBadge
import io.github.gabrielevanger.xodopet.core.designsystem.component.SuccessIllustration
import io.github.gabrielevanger.xodopet.core.designsystem.component.XodoButton
import io.github.gabrielevanger.xodopet.core.designsystem.component.XodoButtonStyle
import io.github.gabrielevanger.xodopet.core.designsystem.component.XodoCard
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme
import io.github.gabrielevanger.xodopet.core.designsystem.theme.highlight
import io.github.gabrielevanger.xodopet.core.domain.checkout.CheckoutError
import io.github.gabrielevanger.xodopet.core.ui.BackTopBar
import io.github.gabrielevanger.xodopet.core.ui.dotSeparated

@Composable
fun CheckoutRoute(
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onPlaced: (PlacedOrderUi) -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val placed = (state as? CheckoutUiState.Ready)?.placed
    LaunchedEffect(placed) { placed?.let(onPlaced) }
    CheckoutScreen(
        state = state,
        actions = CheckoutActions(
            onBack = onBack,
            onSignIn = onSignIn,
            onSelectAddress = viewModel::selectAddress,
            onNewAddress = viewModel::showNewAddress,
            onCancelNewAddress = viewModel::cancelNewAddress,
            onAddressChange = viewModel::onAddressChange,
            onSaveAddress = viewModel::saveAddress,
            onSelectPayment = viewModel::selectPayment,
            onChangeForChange = viewModel::onChangeForChange,
            onNotesChange = viewModel::onNotesChange,
            onSubmit = viewModel::submit,
        ),
    )
}

/** Ações da tela agrupadas, para a assinatura do Composable não virar uma lista de doze parâmetros. */
class CheckoutActions(
    val onBack: () -> Unit = {},
    val onSignIn: () -> Unit = {},
    val onSelectAddress: (String) -> Unit = {},
    val onNewAddress: () -> Unit = {},
    val onCancelNewAddress: () -> Unit = {},
    val onAddressChange: (AddressField, String) -> Unit = { _, _ -> },
    val onSaveAddress: () -> Unit = {},
    val onSelectPayment: (PaymentOption) -> Unit = {},
    val onChangeForChange: (String) -> Unit = {},
    val onNotesChange: (String) -> Unit = {},
    val onSubmit: () -> Unit = {},
)

@Composable
fun CheckoutScreen(state: CheckoutUiState, actions: CheckoutActions) {
    Scaffold(
        containerColor = XodoTheme.colors.background,
        topBar = { BackTopBar(stringResource(R.string.checkout_title), actions.onBack) },
        bottomBar = { if (state is CheckoutUiState.Ready) PlaceOrderBar(state, actions.onSubmit) },
    ) { padding ->
        val modifier = Modifier.padding(padding).consumeWindowInsets(padding)
        when (state) {
            CheckoutUiState.Loading -> LoadingState(stringResource(R.string.checkout_loading), modifier)
            CheckoutUiState.EmptyCart -> MessageState(stringResource(R.string.checkout_empty), modifier)
            is CheckoutUiState.Ready -> CheckoutForm(state, actions, modifier)
        }
    }
}

@Composable
private fun CheckoutForm(state: CheckoutUiState.Ready, actions: CheckoutActions, modifier: Modifier) {
    val spacing = XodoTheme.spacing
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.l),
        modifier = modifier
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.l, vertical = spacing.m),
    ) {
        state.error?.let { error ->
            Notice(error.message())
            if (error == CheckoutError.NotSignedIn) {
                XodoButton(
                    text = stringResource(R.string.checkout_sign_in),
                    onClick = actions.onSignIn,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Section { AddressSection(state, actions) }
        HorizontalDivider(thickness = XodoTheme.sizes.divider)
        Section { PaymentSection(state, actions) }
        FormField(
            value = state.notes,
            onValueChange = actions.onNotesChange,
            label = stringResource(R.string.checkout_notes),
            imeAction = ImeAction.Done,
        )
        DeliveryNotices(state)
    }
}

@Composable
private fun Section(content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(XodoTheme.spacing.s)) { content() }
}

@Composable
private fun AddressSection(state: CheckoutUiState.Ready, actions: CheckoutActions) {
    SectionTitle(stringResource(R.string.checkout_address_title))
    val form = state.newAddress
    if (form == null) {
        Column(verticalArrangement = Arrangement.spacedBy(XodoTheme.spacing.s), modifier = Modifier.selectableGroup()) {
            state.addresses.forEachIndexed { index, address ->
                SelectableOption(
                    title = stringResource(R.string.checkout_address_line, address.street, address.number),
                    details = dotSeparated(address.complement, address.neighborhood, address.reference).ifEmpty {
                        null
                    },
                    selected = address.id == state.selectedAddressId,
                    onSelect = { actions.onSelectAddress(address.id) },
                    // O primeiro é o endereço padrão (pino); os outros, prédio, como no layout.
                    icon = painterResource(
                        if (index ==
                            0
                        ) {
                            DesignR.drawable.ic_map_pin
                        } else {
                            DesignR.drawable.ic_building
                        },
                    ),
                )
            }
        }
        XodoButton(
            text = stringResource(R.string.checkout_address_new),
            onClick = actions.onNewAddress,
            style = XodoButtonStyle.Text,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        NewAddressFields(form, actions, canCancel = state.addresses.isNotEmpty())
    }
}

@Composable
private fun NewAddressFields(form: NewAddressForm, actions: CheckoutActions, canCancel: Boolean) {
    val required = stringResource(R.string.checkout_field_required)
    fun error(field: AddressField) = if (field in form.missing) required else null
    AddressInput(
        form.street,
        AddressField.Street,
        R.string.checkout_field_street,
        error(AddressField.Street),
        form,
        actions,
    )
    AddressInput(
        form.number,
        AddressField.Number,
        R.string.checkout_field_number,
        error(AddressField.Number),
        form,
        actions,
    )
    AddressInput(form.complement, AddressField.Complement, R.string.checkout_field_complement, null, form, actions)
    AddressInput(
        form.neighborhood,
        AddressField.Neighborhood,
        R.string.checkout_field_neighborhood,
        error(AddressField.Neighborhood),
        form,
        actions,
    )
    AddressInput(form.reference, AddressField.Reference, R.string.checkout_field_reference, null, form, actions)
    Text(
        text = stringResource(R.string.checkout_address_point_note, form.placeName),
        style = XodoTheme.typography.caption,
        color = XodoTheme.colors.onSurfaceVariant,
    )
    XodoButton(
        text = stringResource(R.string.checkout_address_save),
        onClick = actions.onSaveAddress,
        loading = form.saving,
        modifier = Modifier.fillMaxWidth(),
    )
    if (canCancel) {
        XodoButton(
            text = stringResource(android.R.string.cancel),
            onClick = actions.onCancelNewAddress,
            style = XodoButtonStyle.Outlined,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AddressInput(
    value: String,
    field: AddressField,
    @StringRes label: Int,
    error: String?,
    form: NewAddressForm,
    actions: CheckoutActions,
) {
    FormField(
        value = value,
        onValueChange = { actions.onAddressChange(field, it) },
        label = stringResource(label),
        error = error,
        enabled = !form.saving,
    )
}

@Composable
private fun PaymentSection(state: CheckoutUiState.Ready, actions: CheckoutActions) {
    SectionTitle(stringResource(R.string.checkout_payment_title))
    Column(verticalArrangement = Arrangement.spacedBy(XodoTheme.spacing.s), modifier = Modifier.selectableGroup()) {
        PaymentOption.entries.forEach { option ->
            val (title, details, icon) = when (option) {
                PaymentOption.Cash -> Triple(
                    R.string.checkout_payment_cash,
                    R.string.checkout_payment_cash_details,
                    DesignR.drawable.ic_banknote,
                )

                PaymentOption.Pix -> Triple(
                    R.string.checkout_payment_pix,
                    R.string.checkout_payment_pix_details,
                    DesignR.drawable.ic_banknote,
                )

                PaymentOption.Card -> Triple(
                    R.string.checkout_payment_card,
                    R.string.checkout_payment_card_details,
                    DesignR.drawable.ic_credit_card,
                )
            }
            SelectableOption(
                title = stringResource(title),
                details = stringResource(details),
                selected = state.payment == option,
                onSelect = { actions.onSelectPayment(option) },
                icon = painterResource(icon),
            )
        }
    }
    if (state.payment == PaymentOption.Cash) {
        FormField(
            value = state.changeFor,
            onValueChange = actions.onChangeForChange,
            label = stringResource(R.string.checkout_change_for),
            error = if (state.changeForInvalid) stringResource(R.string.checkout_change_invalid) else null,
        )
    }
}

/** O total fica embaixo; aqui só aparece o que impede o pedido ou ainda está sendo calculado. */
@Composable
private fun DeliveryNotices(state: CheckoutUiState.Ready) {
    when (val delivery = state.delivery) {
        DeliveryUi.ChooseAddress -> Notice(stringResource(R.string.checkout_delivery_choose_address))
        DeliveryUi.Calculating -> Notice(stringResource(R.string.checkout_delivery_calculating))
        DeliveryUi.OutOfArea -> Notice(stringResource(R.string.checkout_delivery_out_of_area))
        DeliveryUi.Error -> Notice(stringResource(R.string.checkout_delivery_error))
        is DeliveryUi.Fee -> if (!delivery.storeIsOpen) Notice(stringResource(R.string.checkout_error_store_closed))
    }
    state.missingForMinOrder?.let { Notice(stringResource(R.string.checkout_min_order, it, state.minOrder)) }
}

@Composable
private fun PlaceOrderBar(state: CheckoutUiState.Ready, onSubmit: () -> Unit) {
    Surface(color = XodoTheme.colors.surfaceContainerLowest) {
        Column(
            verticalArrangement = Arrangement.spacedBy(XodoTheme.spacing.m),
            modifier = Modifier.navigationBarsPadding().padding(XodoTheme.spacing.l),
        ) {
            val delivery = state.delivery as? DeliveryUi.Fee
            if (delivery != null) {
                SummaryRow(
                    stringResource(R.string.checkout_delivery),
                    if (delivery.isFree) stringResource(R.string.checkout_delivery_free) else delivery.value,
                )
            }
            state.total?.let { SummaryRow(stringResource(R.string.checkout_total), it, emphasis = true) }
            XodoButton(
                text = stringResource(R.string.checkout_place_order),
                onClick = onSubmit,
                enabled = state.canPlaceOrder,
                loading = state.submitting,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, emphasis: Boolean = false) {
    val style = if (emphasis) XodoTheme.typography.sectionTitle else XodoTheme.typography.body
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = style, modifier = Modifier.weight(1f))
        Text(text = value, style = if (emphasis) style else XodoTheme.typography.price)
    }
}

@Composable
internal fun CheckoutError.message(): String = stringResource(
    when (this) {
        CheckoutError.NotSignedIn -> R.string.checkout_error_not_signed_in
        CheckoutError.StoreClosed -> R.string.checkout_error_store_closed
        CheckoutError.OutOfDeliveryArea -> R.string.checkout_error_out_of_area
        CheckoutError.ItemUnavailable -> R.string.checkout_error_item_unavailable
        CheckoutError.InsufficientStock -> R.string.checkout_error_insufficient_stock
        CheckoutError.BelowMinOrder -> R.string.checkout_error_below_min_order
        CheckoutError.PriceChanged -> R.string.checkout_error_price_changed
        CheckoutError.ChangeTooLow -> R.string.checkout_error_change_too_low
        CheckoutError.EmptyCart -> R.string.checkout_error_empty_cart
        CheckoutError.Network -> R.string.checkout_error_network
        CheckoutError.Unknown -> R.string.checkout_error_unknown
    },
)

@Composable
fun OrderPlacedScreen(
    number: Long,
    storeName: String,
    total: String,
    onBackToStorefront: () -> Unit,
    onTrackOrder: () -> Unit = {},
) {
    val spacing = XodoTheme.spacing
    Scaffold(
        containerColor = XodoTheme.colors.background,
        topBar = { BackTopBar(stringResource(R.string.placed_title), onBackToStorefront) },
        bottomBar = {
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
                modifier = Modifier.navigationBarsPadding().padding(horizontal = spacing.l, vertical = spacing.m),
            ) {
                XodoButton(
                    text = stringResource(R.string.placed_track_order),
                    onClick = onTrackOrder,
                    modifier = Modifier.fillMaxWidth(),
                )
                XodoButton(
                    text = stringResource(R.string.placed_back_to_storefront),
                    onClick = onBackToStorefront,
                    style = XodoButtonStyle.Text,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.l, vertical = spacing.l),
        ) {
            SuccessIllustration(painterResource(DesignR.drawable.ic_shopping_bag))
            StatusBadge(stringResource(R.string.placed_status), BadgeTone.Positive)
            Text(
                text = stringResource(R.string.placed_headline, number),
                style = XodoTheme.typography.headline,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.placed_text, storeName),
                style = XodoTheme.typography.message,
                color = XodoTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            XodoCard(shape = XodoTheme.shapes.panel) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.s),
                ) {
                    Icon(
                        painter = painterResource(DesignR.drawable.ic_store),
                        contentDescription = null,
                        tint = XodoTheme.colors.highlight,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs), modifier = Modifier.weight(1f)) {
                        Text(text = storeName, style = XodoTheme.typography.cardTitle)
                        Text(
                            text = stringResource(R.string.placed_total, total),
                            style = XodoTheme.typography.caption,
                            color = XodoTheme.colors.onSurfaceVariant,
                        )
                    }
                    Icon(
                        painter = painterResource(DesignR.drawable.ic_clock),
                        contentDescription = null,
                        tint = XodoTheme.colors.primary,
                        modifier = Modifier.size(XodoTheme.sizes.icon),
                    )
                }
            }
        }
    }
}
