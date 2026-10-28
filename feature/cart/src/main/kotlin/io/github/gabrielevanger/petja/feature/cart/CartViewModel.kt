package io.github.gabrielevanger.petja.feature.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gabrielevanger.petja.core.domain.DiagnosticLog
import io.github.gabrielevanger.petja.core.domain.account.AccountRepository
import io.github.gabrielevanger.petja.core.domain.account.AccountState
import io.github.gabrielevanger.petja.core.domain.cart.CartActions
import io.github.gabrielevanger.petja.core.domain.cart.CartRepository
import io.github.gabrielevanger.petja.core.domain.delivery.DeliveryPlaceRepository
import io.github.gabrielevanger.petja.core.domain.delivery.DeliveryQuote
import io.github.gabrielevanger.petja.core.domain.delivery.QuoteDelivery
import io.github.gabrielevanger.petja.core.domain.model.Cart
import io.github.gabrielevanger.petja.core.domain.model.DeliveryPlace
import io.github.gabrielevanger.petja.core.domain.model.Money
import io.github.gabrielevanger.petja.core.ui.formatBrl
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

/** Resultado da consulta de entrega; separado de DeliveryUi para o cálculo do total usar o valor em centavos. */
private sealed interface QuoteState {
    data object Calculating : QuoteState

    data object NoPlace : QuoteState

    data object Error : QuoteState

    data class Ready(val quote: DeliveryQuote?) : QuoteState
}

@HiltViewModel
class CartViewModel @Inject constructor(
    cartRepository: CartRepository,
    deliveryPlaces: DeliveryPlaceRepository,
    private val quoteDelivery: QuoteDelivery,
    private val cartActions: CartActions,
    accounts: AccountRepository,
    private val log: DiagnosticLog,
) : ViewModel() {

    /**
     * A taxa só depende da loja e do lugar: mudar quantidades não refaz a consulta.
     * Trocar de loja ou de lugar cancela a consulta anterior.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val quote: Flow<QuoteState> = combine(
        cartRepository.cart.map { it?.storeId }.distinctUntilChanged(),
        deliveryPlaces.place,
    ) { storeId, place -> storeId to place }
        .distinctUntilChanged()
        .transformLatest { (storeId, place) ->
            when {
                storeId == null -> Unit

                place == null -> emit(QuoteState.NoPlace)

                else -> {
                    emit(QuoteState.Calculating)
                    emit(fetchQuote(storeId, place))
                }
            }
        }

    // Estado derivado só de flows (carrinho, lugar, conta e taxa): o stateIn(WhileSubscribed) já liga quando a
    // tela aparece e desliga quando ela sai, o mesmo que OnStart e OnStop fariam.
    // Com o carrinho vazio a consulta não emite nada; o valor inicial evita que o combine fique esperando.
    val uiState: StateFlow<CartUiState> = combine(
        cartRepository.cart,
        deliveryPlaces.place,
        quote.onStart { emit(QuoteState.Calculating) },
        accounts.state,
    ) { cart, place, quote, account ->
        cart?.toUiState(place, quote)?.copy(signedIn = account is AccountState.SignedIn) ?: CartUiState.Empty
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CartUiState.Loading)

    private val _events = Channel<CartEvent>(Channel.BUFFERED)
    val events: Flow<CartEvent> = _events.receiveAsFlow()

    fun onAction(action: CartAction) {
        when (action) {
            CartAction.OnBackClick -> send(CartEvent.Close)

            is CartAction.OnIncrease -> launch { cartActions.setQuantity(action.variantId, action.quantity + 1) }

            is CartAction.OnDecrease -> launch { cartActions.setQuantity(action.variantId, action.quantity - 1) }

            CartAction.OnClearClick -> launch { cartActions.clear() }

            CartAction.OnContinueClick -> {
                val loaded = uiState.value as? CartUiState.Loaded ?: return
                if (!loaded.canCheckout) return
                send(if (loaded.signedIn) CartEvent.OpenCheckout else CartEvent.OpenSignIn)
            }
        }
    }

    private fun send(event: CartEvent) {
        _events.trySend(event)
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private suspend fun fetchQuote(storeId: String, place: DeliveryPlace): QuoteState = try {
        QuoteState.Ready(quoteDelivery(storeId, place.point))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        log.warn("Falha ao calcular a entrega da loja $storeId para ${place.name}", e)
        QuoteState.Error
    }
}

private fun Cart.toUiState(place: DeliveryPlace?, quote: QuoteState): CartUiState.Loaded {
    val fee = (quote as? QuoteState.Ready)?.quote?.fee
    return CartUiState.Loaded(
        storeName = storeName,
        placeName = place?.name,
        lines = lines.map {
            CartLineUi(
                variantId = it.variantId,
                productName = it.productName,
                variantLabel = it.variantLabel,
                unitPrice = it.unitPrice.formatBrl(),
                total = it.total.formatBrl(),
                quantity = it.quantity,
                canIncrease = it.quantity < it.maxQuantity,
            )
        }.toImmutableList(),
        subtotal = subtotal.formatBrl(),
        delivery = when (quote) {
            QuoteState.Calculating -> DeliveryUi.Calculating

            QuoteState.NoPlace -> DeliveryUi.NoPlace

            QuoteState.Error -> DeliveryUi.Error

            is QuoteState.Ready -> quote.quote?.let {
                DeliveryUi.Fee(it.fee.formatBrl(), isFree = it.fee == Money.ZERO, storeIsOpen = it.storeIsOpen)
            } ?: DeliveryUi.OutOfArea
        },
        total = fee?.let { (subtotal + it).formatBrl() },
        minOrder = minOrder.formatBrl(),
        missingForMinOrder = missingForMinOrder.takeIf { !meetsMinOrder }?.formatBrl(),
    )
}

/** Mantém o estado vivo por alguns segundos sem tela, para sobreviver à rotação sem refazer a consulta. */
private const val STOP_TIMEOUT_MILLIS = 5_000L
