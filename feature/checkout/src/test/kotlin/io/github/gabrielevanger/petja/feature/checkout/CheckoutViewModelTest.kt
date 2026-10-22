package io.github.gabrielevanger.petja.feature.checkout

import io.github.gabrielevanger.petja.core.domain.DiagnosticLog
import io.github.gabrielevanger.petja.core.domain.cart.CartActions
import io.github.gabrielevanger.petja.core.domain.checkout.CheckoutError
import io.github.gabrielevanger.petja.core.domain.checkout.PaymentChoice
import io.github.gabrielevanger.petja.core.domain.checkout.PlaceOrder
import io.github.gabrielevanger.petja.core.domain.delivery.QuoteDelivery
import io.github.gabrielevanger.petja.core.domain.model.Cart
import io.github.gabrielevanger.petja.core.domain.model.CartLine
import io.github.gabrielevanger.petja.core.domain.model.Money
import io.github.gabrielevanger.petja.core.domain.testing.CENTRO_PLACE
import io.github.gabrielevanger.petja.core.domain.testing.FakeCartRepository
import io.github.gabrielevanger.petja.core.domain.testing.FakeCheckoutRepository
import io.github.gabrielevanger.petja.core.domain.testing.FakeDeliveryPlaceRepository
import io.github.gabrielevanger.petja.core.domain.testing.FakeStorefrontRepository
import io.github.gabrielevanger.petja.core.domain.testing.aStore
import io.github.gabrielevanger.petja.core.domain.testing.anAddress
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val line = CartLine("1kg", "golden", "Ração Golden", "1 kg", Money(3090), quantity = 2, maxQuantity = 5)
    private val cart = FakeCartRepository(Cart("store-1", "Petshop Lavrado", Money(3000), listOf(line)))
    private val checkout = FakeCheckoutRepository(savedAddresses = listOf(anAddress()))
    private val places = FakeDeliveryPlaceRepository(CENTRO_PLACE)
    private val storefront = FakeStorefrontRepository(stores = listOf(aStore(id = "store-1", deliveryFeeCents = 500)))
    private val warnings = mutableListOf<String>()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = CheckoutViewModel(
        cart,
        checkout,
        places,
        QuoteDelivery(storefront),
        PlaceOrder(checkout, CartActions(cart)),
        DiagnosticLog { message, _ -> warnings += message },
    )

    private fun CheckoutViewModel.ready() = assertInstanceOf(CheckoutUiState.Ready::class.java, uiState.value)

    @Test
    fun `loads the saved address and its fee`() = runTest(dispatcher) {
        val viewModel = viewModel()
        assertEquals(CheckoutUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        val state = viewModel.ready()
        assertEquals("casa", state.selectedAddressId)
        assertNull(state.newAddress)
        assertEquals(DeliveryUi.Fee("R$ 5,00", isFree = false, storeIsOpen = true), state.delivery)
        assertEquals("R$ 66,80", state.total)
        assertTrue(state.canPlaceOrder)
    }

    @Test
    fun `empty cart`() = runTest(dispatcher) {
        cart.update { null }
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(CheckoutUiState.EmptyCart, viewModel.uiState.value)
    }

    @Test
    fun `without addresses asks for one, prefilled with the place`() = runTest(dispatcher) {
        checkout.savedAddresses = emptyList()
        val viewModel = viewModel()
        advanceUntilIdle()

        val form = viewModel.ready().newAddress
        assertEquals("Centro", form?.neighborhood)
        assertFalse(viewModel.ready().canPlaceOrder)

        viewModel.saveAddress()
        assertEquals(setOf(AddressField.Street, AddressField.Number), viewModel.ready().newAddress?.missing)

        viewModel.onAddressChange(AddressField.Street, "Rua Nova")
        viewModel.onAddressChange(AddressField.Number, "12")
        viewModel.onAddressChange(AddressField.Complement, "Apto 2")
        viewModel.onAddressChange(AddressField.Reference, "Portão azul")
        viewModel.onAddressChange(AddressField.Neighborhood, "Caçari")
        viewModel.saveAddress()
        advanceUntilIdle()

        val state = viewModel.ready()
        assertNull(state.newAddress)
        assertEquals("novo-0", state.selectedAddressId)
        assertEquals("Apto 2", checkout.savedAddresses.first().complement)
        assertEquals(CENTRO_PLACE.point, checkout.savedAddresses.first().location)
        assertTrue(state.canPlaceOrder)
    }

    @Test
    fun `can open and cancel a new address when there are saved ones`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.showNewAddress()
        assertEquals("Centro", viewModel.ready().newAddress?.placeName)
        assertFalse(viewModel.ready().canPlaceOrder)

        viewModel.cancelNewAddress()
        assertNull(viewModel.ready().newAddress)
        viewModel.selectAddress("casa")
        viewModel.selectAddress("nao-existe")
        advanceUntilIdle()
        assertEquals("casa", viewModel.ready().selectedAddressId)
    }

    @Test
    fun `address outside the delivery area blocks the order`() = runTest(dispatcher) {
        storefront.stores = emptyList()
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(DeliveryUi.OutOfArea, viewModel.ready().delivery)
        assertFalse(viewModel.ready().canPlaceOrder)
    }

    @Test
    fun `fee failure is logged`() = runTest(dispatcher) {
        storefront.failWith = IOException("sem rede")
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(DeliveryUi.Error, viewModel.ready().delivery)
        assertEquals(1, warnings.size)
    }

    @Test
    fun `places the order with cash and change, then shows it`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.selectPayment(PaymentOption.Cash)
        viewModel.onChangeForChange("100")
        viewModel.onNotesChange("Tocar a campainha")
        viewModel.submit()
        advanceUntilIdle()

        val call = checkout.orders.single()
        assertEquals(PaymentChoice.Cash(Money(10000)), call.payment)
        assertEquals(Money(6680), call.expectedTotal)
        assertEquals("Tocar a campainha", call.notes)
        assertEquals(PlacedOrderUi("pedido-1", 1, "Petshop Lavrado", "R$ 66,80"), viewModel.ready().placed)
        assertNull(cart.cart.value)
    }

    @Test
    fun `invalid change text is pointed out without calling the server`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.selectPayment(PaymentOption.Cash)
        viewModel.onChangeForChange("cem reais")
        viewModel.submit()

        assertTrue(viewModel.ready().changeForInvalid)
        assertTrue(checkout.orders.isEmpty())
    }

    @Test
    fun `card and pix`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.selectPayment(PaymentOption.Card)
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(PaymentChoice.CardOnDelivery, checkout.orders.single().payment)
    }

    @Test
    fun `price changed shows the error and asks for the fee again`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        checkout.failWith = CheckoutError.PriceChanged

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(CheckoutError.PriceChanged, viewModel.ready().error)
        assertEquals(2, storefront.storesNearCalls.size)
        assertFalse(viewModel.ready().submitting)
    }

    @Test
    fun `signed out customer sees the reason`() = runTest(dispatcher) {
        checkout.failAddressesWith = CheckoutError.NotSignedIn
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(CheckoutError.NotSignedIn, viewModel.ready().error)
        assertTrue(warnings.isEmpty())
    }

    @Test
    fun `network failure saving the address is logged and shown`() = runTest(dispatcher) {
        checkout.savedAddresses = emptyList()
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onAddressChange(AddressField.Street, "Rua Nova")
        viewModel.onAddressChange(AddressField.Number, "12")
        checkout.failWith = CheckoutError.Network

        viewModel.saveAddress()
        advanceUntilIdle()

        assertEquals(CheckoutError.Network, viewModel.ready().error)
        assertFalse(viewModel.ready().newAddress!!.saving)
        assertEquals(1, warnings.size)
    }
}
