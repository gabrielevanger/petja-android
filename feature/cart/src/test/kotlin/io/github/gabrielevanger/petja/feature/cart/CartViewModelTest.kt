package io.github.gabrielevanger.petja.feature.cart

import app.cash.turbine.test
import io.github.gabrielevanger.petja.core.domain.DiagnosticLog
import io.github.gabrielevanger.petja.core.domain.account.AccountState
import io.github.gabrielevanger.petja.core.domain.cart.CartActions
import io.github.gabrielevanger.petja.core.domain.delivery.QuoteDelivery
import io.github.gabrielevanger.petja.core.domain.model.Cart
import io.github.gabrielevanger.petja.core.domain.model.CartLine
import io.github.gabrielevanger.petja.core.domain.model.Money
import io.github.gabrielevanger.petja.core.domain.testing.ANA
import io.github.gabrielevanger.petja.core.domain.testing.CENTRO_PLACE
import io.github.gabrielevanger.petja.core.domain.testing.FakeAccountRepository
import io.github.gabrielevanger.petja.core.domain.testing.FakeCartRepository
import io.github.gabrielevanger.petja.core.domain.testing.FakeDeliveryPlaceRepository
import io.github.gabrielevanger.petja.core.domain.testing.FakeStorefrontRepository
import io.github.gabrielevanger.petja.core.domain.testing.aStore
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val golden = CartLine("1kg", "golden", "Ração Golden", "1 kg", Money(3090), quantity = 1, maxQuantity = 2)
    private val cart = FakeCartRepository(Cart("store-1", "Petshop Lavrado", Money(5000), listOf(golden)))
    private val places = FakeDeliveryPlaceRepository(CENTRO_PLACE)
    private val storefront = FakeStorefrontRepository(stores = listOf(aStore(id = "store-1", deliveryFeeCents = 500)))
    private val warnings = mutableListOf<String>()
    private val accounts = FakeAccountRepository()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel() = CartViewModel(
        cart,
        places,
        QuoteDelivery(storefront),
        CartActions(cart),
        accounts,
        DiagnosticLog { message, _ -> warnings += message },
    ).also { it.uiState.launchIn(backgroundScope) }

    private fun CartViewModel.loaded() = assertInstanceOf(CartUiState.Loaded::class.java, uiState.value)

    @Test
    fun `shows lines, fee, total and what is missing for the minimum`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.loaded()
        assertEquals(listOf("Ração Golden"), state.lines.map { it.productName })
        assertEquals("R$ 30,90", state.subtotal)
        assertEquals(DeliveryUi.Fee("R$ 5,00", isFree = false, storeIsOpen = true), state.delivery)
        assertEquals("R$ 35,90", state.total)
        assertEquals("R$ 19,10", state.missingForMinOrder)
    }

    @Test
    fun `changing quantities does not ask for the fee again`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(CartAction.OnIncrease("1kg", 1))
        advanceUntilIdle()

        assertEquals(2, viewModel.loaded().lines.single().quantity)
        assertEquals(false, viewModel.loaded().lines.single().canIncrease, "chegou ao estoque")
        assertEquals("R$ 66,80", viewModel.loaded().total)
        assertEquals(1, storefront.storesNearCalls.size)
    }

    @Test
    fun `store out of range for the place`() = runTest(dispatcher) {
        storefront.stores = emptyList()
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(DeliveryUi.OutOfArea, viewModel.loaded().delivery)
        assertNull(viewModel.loaded().total)
    }

    @Test
    fun `no place chosen yet`() = runTest(dispatcher) {
        val noPlace = FakeDeliveryPlaceRepository()
        val viewModel = CartViewModel(
            cart,
            noPlace,
            QuoteDelivery(storefront),
            CartActions(cart),
            accounts,
            DiagnosticLog {
                    _,
                    _,
                ->
            },
        )
            .also { it.uiState.launchIn(backgroundScope) }
        advanceUntilIdle()

        assertEquals(DeliveryUi.NoPlace, viewModel.loaded().delivery)
    }

    @Test
    fun `fee error is logged and shown`() = runTest(dispatcher) {
        storefront.failWith = IOException("sem rede")
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(DeliveryUi.Error, viewModel.loaded().delivery)
        assertEquals(1, warnings.size)
    }

    @Test
    fun `removing the last item empties the cart`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(CartAction.OnDecrease("1kg", 1))
        advanceUntilIdle()

        assertEquals(CartUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `clear empties the cart`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(CartAction.OnClearClick)
        advanceUntilIdle()

        assertEquals(CartUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `knows whether the customer is signed in`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(false, viewModel.loaded().signedIn)
        assertEquals(false, viewModel.loaded().canCheckout, "abaixo do pedido mínimo")

        accounts.signIn(ANA.email, "segredo123")
        viewModel.onAction(CartAction.OnIncrease("1kg", 1))
        advanceUntilIdle()

        assertEquals(true, viewModel.loaded().signedIn)
        assertEquals(true, viewModel.loaded().canCheckout)
        assertEquals(AccountState.SignedIn(ANA), accounts.state.value)
    }

    @Test
    fun `continue goes to sign in or checkout, and only when the order can go`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onAction(CartAction.OnContinueClick)
            expectNoEvents()

            cart.update { it?.copy(lines = listOf(golden.copy(quantity = 2))) }
            advanceUntilIdle()
            viewModel.onAction(CartAction.OnContinueClick)
            assertEquals(CartEvent.OpenSignIn, awaitItem())

            accounts.signIn(ANA.email, "segredo123")
            advanceUntilIdle()
            viewModel.onAction(CartAction.OnContinueClick)
            assertEquals(CartEvent.OpenCheckout, awaitItem())

            viewModel.onAction(CartAction.OnBackClick)
            assertEquals(CartEvent.Close, awaitItem())
        }
    }
}
