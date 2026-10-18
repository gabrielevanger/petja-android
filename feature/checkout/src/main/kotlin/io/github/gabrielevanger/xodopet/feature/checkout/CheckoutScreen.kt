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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gabrielevanger.xodopet.core.designsystem.component.FormField
import io.github.gabrielevanger.xodopet.core.designsystem.component.LoadingState
import io.github.gabrielevanger.xodopet.core.designsystem.component.MessageState
import io.github.gabrielevanger.xodopet.core.designsystem.component.Notice
import io.github.gabrielevanger.xodopet.core.designsystem.component.SectionTitle
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme
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
        verticalArrangement = Arrangement.spacedBy(spacing.s),
        modifier = modifier.imePadding().verticalScroll(rememberScrollState()).padding(spacing.m),
    ) {
        Text(
            text = stringResource(R.string.checkout_from_store, state.storeName),
            style = XodoTheme.typography.sectionTitle,
        )
        state.error?.let { error ->
            Notice(error.message())
            if (error == CheckoutError.NotSignedIn) {
                Button(onClick = actions.onSignIn, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.checkout_sign_in))
                }
            }
        }
        AddressSection(state, actions)
        PaymentSection(state, actions)
        FormField(
            value = state.notes,
            onValueChange = actions.onNotesChange,
            label = stringResource(R.string.checkout_notes),
            imeAction = ImeAction.Done,
        )
        Summary(state)
    }
}

@Composable
private fun AddressSection(state: CheckoutUiState.Ready, actions: CheckoutActions) {
    SectionTitle(stringResource(R.string.checkout_address_title))
    val form = state.newAddress
    if (form == null) {
        Column(Modifier.selectableGroup()) {
            state.addresses.forEach { address ->
                ChoiceRow(
                    selected = address.id == state.selectedAddressId,
                    onClick = { actions.onSelectAddress(address.id) },
                    title = stringResource(R.string.checkout_address_line, address.street, address.number),
                    details = dotSeparated(address.complement, address.neighborhood, address.reference).ifEmpty {
                        null
                    },
                )
            }
        }
        TextButton(onClick = actions.onNewAddress) { Text(stringResource(R.string.checkout_address_new)) }
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
    Row(horizontalArrangement = Arrangement.spacedBy(XodoTheme.spacing.xs)) {
        Button(onClick = actions.onSaveAddress, enabled = !form.saving) {
            Text(stringResource(R.string.checkout_address_save))
        }
        if (canCancel) {
            OutlinedButton(onClick = actions.onCancelNewAddress) {
                Text(stringResource(android.R.string.cancel))
            }
        }
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
    Column(Modifier.selectableGroup()) {
        PaymentOption.entries.forEach { option ->
            ChoiceRow(
                selected = state.payment == option,
                onClick = { actions.onSelectPayment(option) },
                title = stringResource(
                    when (option) {
                        PaymentOption.Cash -> R.string.checkout_payment_cash
                        PaymentOption.Pix -> R.string.checkout_payment_pix
                        PaymentOption.Card -> R.string.checkout_payment_card
                    },
                ),
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

/** Opção de escolha única com a linha inteira tocável, como pede a acessibilidade do Material. */
@Composable
private fun ChoiceRow(selected: Boolean, onClick: () -> Unit, title: String, details: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = XodoTheme.spacing.xxs),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.padding(start = XodoTheme.spacing.xs)) {
            Text(text = title, style = XodoTheme.typography.body)
            details?.let {
                Text(text = it, style = XodoTheme.typography.caption, color = XodoTheme.colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Summary(state: CheckoutUiState.Ready) {
    SectionTitle(stringResource(R.string.checkout_summary_title))
    SummaryRow(stringResource(R.string.checkout_subtotal), state.subtotal)
    when (val delivery = state.delivery) {
        DeliveryUi.ChooseAddress -> Notice(stringResource(R.string.checkout_delivery_choose_address))

        DeliveryUi.Calculating -> SummaryRow(
            stringResource(R.string.checkout_delivery),
            stringResource(R.string.checkout_delivery_calculating),
        )

        DeliveryUi.OutOfArea -> Notice(stringResource(R.string.checkout_delivery_out_of_area))

        DeliveryUi.Error -> Notice(stringResource(R.string.checkout_delivery_error))

        is DeliveryUi.Fee -> {
            SummaryRow(
                stringResource(R.string.checkout_delivery),
                if (delivery.isFree) stringResource(R.string.checkout_delivery_free) else delivery.value,
            )
            if (!delivery.storeIsOpen) Notice(stringResource(R.string.checkout_error_store_closed))
        }
    }
    state.total?.let { SummaryRow(stringResource(R.string.checkout_total), it) }
    state.missingForMinOrder?.let { Notice(stringResource(R.string.checkout_min_order, it, state.minOrder)) }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = XodoTheme.typography.body, modifier = Modifier.weight(1f))
        Text(text = value, style = XodoTheme.typography.price)
    }
}

@Composable
private fun PlaceOrderBar(state: CheckoutUiState.Ready, onSubmit: () -> Unit) {
    Surface(color = XodoTheme.colors.surfaceContainerHigh) {
        Button(
            onClick = onSubmit,
            enabled = state.canPlaceOrder,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(XodoTheme.spacing.m),
        ) {
            Text(
                state.total?.let { stringResource(R.string.checkout_place_order, it) }
                    ?: stringResource(R.string.checkout_place_order_disabled),
            )
        }
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
fun OrderPlacedScreen(number: Long, storeName: String, total: String, onBackToStorefront: () -> Unit) {
    Scaffold(containerColor = XodoTheme.colors.background) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(XodoTheme.spacing.s, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding).padding(XodoTheme.spacing.l),
        ) {
            Text(text = stringResource(R.string.placed_title), style = XodoTheme.typography.screenTitle)
            Text(text = stringResource(R.string.placed_number, number), style = XodoTheme.typography.sectionTitle)
            Text(text = stringResource(R.string.placed_text, storeName, total), style = XodoTheme.typography.body)
            Text(
                text = stringResource(R.string.placed_tracking_soon),
                style = XodoTheme.typography.caption,
                color = XodoTheme.colors.onSurfaceVariant,
            )
            Button(onClick = onBackToStorefront, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.placed_back_to_storefront))
            }
        }
    }
}
