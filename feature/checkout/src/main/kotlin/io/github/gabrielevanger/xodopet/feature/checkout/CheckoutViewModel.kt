package io.github.gabrielevanger.xodopet.feature.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gabrielevanger.xodopet.core.domain.DiagnosticLog
import io.github.gabrielevanger.xodopet.core.domain.cart.CartRepository
import io.github.gabrielevanger.xodopet.core.domain.checkout.CheckoutError
import io.github.gabrielevanger.xodopet.core.domain.checkout.CheckoutException
import io.github.gabrielevanger.xodopet.core.domain.checkout.CheckoutRepository
import io.github.gabrielevanger.xodopet.core.domain.checkout.PaymentChoice
import io.github.gabrielevanger.xodopet.core.domain.checkout.PlaceOrder
import io.github.gabrielevanger.xodopet.core.domain.delivery.DeliveryPlaceRepository
import io.github.gabrielevanger.xodopet.core.domain.delivery.QuoteDelivery
import io.github.gabrielevanger.xodopet.core.domain.model.Address
import io.github.gabrielevanger.xodopet.core.domain.model.Cart
import io.github.gabrielevanger.xodopet.core.domain.model.GeoPoint
import io.github.gabrielevanger.xodopet.core.domain.model.Money
import io.github.gabrielevanger.xodopet.core.domain.model.NewAddress
import io.github.gabrielevanger.xodopet.core.ui.formatBrl
import io.github.gabrielevanger.xodopet.core.ui.parseBrl
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val checkout: CheckoutRepository,
    private val deliveryPlaces: DeliveryPlaceRepository,
    private val quoteDelivery: QuoteDelivery,
    private val placeOrder: PlaceOrder,
    private val log: DiagnosticLog,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Loading)
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    private var cart: Cart? = null
    private var addresses: List<Address> = emptyList()

    /** Ponto do endereço novo enquanto não há mapa (RF-11): o lugar escolhido na vitrine. */
    private var placePoint: GeoPoint? = null
    private var placeName: String = ""
    private var fee: Money? = null
    private var quoting: Job? = null

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val cart = cartRepository.cart.first() ?: return run { _uiState.value = CheckoutUiState.EmptyCart }
        this.cart = cart
        val place = deliveryPlaces.place.first()
        placePoint = place?.point
        placeName = place?.name.orEmpty()
        var error: CheckoutError? = null
        addresses = try {
            checkout.addresses()
        } catch (e: CheckoutException) {
            error = e.error
            emptyList()
        }
        val selected = addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()
        _uiState.value = CheckoutUiState.Ready(
            storeName = cart.storeName,
            itemCount = cart.itemCount,
            addresses = addresses.map { it.toUi() }.toImmutableList(),
            selectedAddressId = selected?.id,
            newAddress = if (selected ==
                null
            ) {
                NewAddressForm(neighborhood = placeName, placeName = placeName)
            } else {
                null
            },
            subtotal = cart.subtotal.formatBrl(),
            minOrder = cart.minOrder.formatBrl(),
            missingForMinOrder = cart.missingForMinOrder.takeIf { !cart.meetsMinOrder }?.formatBrl(),
            error = error,
        )
        selected?.let(::quote)
    }

    fun selectAddress(id: String) {
        val address = addresses.firstOrNull { it.id == id } ?: return
        updateReady { it.copy(selectedAddressId = id, newAddress = null, error = null) }
        quote(address)
    }

    fun showNewAddress() = updateReady {
        it.copy(newAddress = NewAddressForm(neighborhood = placeName, placeName = placeName), error = null)
    }

    fun cancelNewAddress() = updateReady { if (addresses.isEmpty()) it else it.copy(newAddress = null) }

    fun onAddressChange(field: AddressField, value: String) = updateReady { state ->
        val form = state.newAddress ?: return@updateReady state
        val changed = when (field) {
            AddressField.Street -> form.copy(street = value)
            AddressField.Number -> form.copy(number = value)
            AddressField.Neighborhood -> form.copy(neighborhood = value)
            AddressField.Complement -> form.copy(complement = value)
            AddressField.Reference -> form.copy(reference = value)
        }
        state.copy(newAddress = changed.copy(missing = changed.missing - field), error = null)
    }

    fun saveAddress() {
        val form = (_uiState.value as? CheckoutUiState.Ready)?.newAddress ?: return
        val missing = buildSet {
            if (form.street.isBlank()) add(AddressField.Street)
            if (form.number.isBlank()) add(AddressField.Number)
            if (form.neighborhood.isBlank()) add(AddressField.Neighborhood)
        }
        val point = placePoint
        if (missing.isNotEmpty() || point == null || form.saving) {
            updateReady { it.copy(newAddress = form.copy(missing = missing)) }
            return
        }
        updateReady { it.copy(newAddress = form.copy(saving = true)) }
        viewModelScope.launch {
            try {
                val saved = checkout.addAddress(
                    NewAddress(
                        street = form.street.trim(),
                        number = form.number.trim(),
                        complement = form.complement.trim().ifEmpty { null },
                        neighborhood = form.neighborhood.trim(),
                        reference = form.reference.trim().ifEmpty { null },
                        location = point,
                    ),
                )
                addresses = listOf(saved) + addresses.map { it.copy(isDefault = false) }
                updateReady { it.copy(addresses = addresses.map { a -> a.toUi() }.toImmutableList()) }
                selectAddress(saved.id)
            } catch (e: CheckoutException) {
                logIfUnexpected(e)
                updateReady { it.copy(newAddress = form.copy(saving = false), error = e.error) }
            }
        }
    }

    fun selectPayment(option: PaymentOption) = updateReady {
        it.copy(payment = option, changeForInvalid = false, error = null)
    }

    fun onChangeForChange(text: String) = updateReady {
        it.copy(changeFor = text, changeForInvalid = false, error = null)
    }

    fun onNotesChange(text: String) = updateReady { it.copy(notes = text) }

    fun submit() {
        val state = _uiState.value as? CheckoutUiState.Ready ?: return
        if (!state.canPlaceOrder) return
        val address = addresses.firstOrNull { it.id == state.selectedAddressId } ?: return
        val fee = fee ?: return
        val payment = when (state.payment) {
            PaymentOption.Pix -> PaymentChoice.PixOnDelivery

            PaymentOption.Card -> PaymentChoice.CardOnDelivery

            PaymentOption.Cash -> {
                val change = state.changeFor.takeIf { it.isNotBlank() }?.let {
                    parseBrl(it)
                        ?: return markChangeInvalid()
                }
                PaymentChoice.Cash(change)
            }
        }
        updateReady { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            try {
                // O carrinho do momento do pedido, não o de quando a tela abriu.
                val current = cartRepository.cart.first() ?: throw CheckoutException(CheckoutError.EmptyCart)
                val order = placeOrder(current, address, payment, state.notes, fee)
                updateReady {
                    it.copy(
                        submitting = false,
                        placed = PlacedOrderUi(order.id, order.number, current.storeName, order.total.formatBrl()),
                    )
                }
            } catch (e: CheckoutException) {
                logIfUnexpected(e)
                updateReady { it.copy(submitting = false, error = e.error) }
                if (e.error == CheckoutError.PriceChanged) quote(address)
            }
        }
    }

    private fun markChangeInvalid() = updateReady { it.copy(changeForInvalid = true) }

    /** Taxa e total para o endereço escolhido. Trocar de endereço cancela a consulta anterior. */
    private fun quote(address: Address) {
        val cart = cart ?: return
        quoting?.cancel()
        fee = null
        updateReady { it.copy(delivery = DeliveryUi.Calculating, total = null) }
        quoting = viewModelScope.launch {
            val delivery = try {
                quoteDelivery(cart.storeId, address.location)?.let { quote ->
                    fee = quote.fee
                    DeliveryUi.Fee(
                        quote.fee.formatBrl(),
                        isFree = quote.fee == Money.ZERO,
                        storeIsOpen = quote.storeIsOpen,
                    )
                } ?: DeliveryUi.OutOfArea
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.warn("Falha ao calcular a entrega no checkout", e)
                DeliveryUi.Error
            }
            updateReady { it.copy(delivery = delivery, total = fee?.let { f -> (cart.subtotal + f).formatBrl() }) }
        }
    }

    private fun logIfUnexpected(e: CheckoutException) {
        if (e.error == CheckoutError.Unknown || e.error == CheckoutError.Network) log.warn("Falha no checkout", e)
    }

    private fun updateReady(change: (CheckoutUiState.Ready) -> CheckoutUiState.Ready) = _uiState.update { state ->
        if (state is CheckoutUiState.Ready) change(state) else state
    }

    private fun Address.toUi() = AddressUi(id, street, number, complement, neighborhood, reference)
}
