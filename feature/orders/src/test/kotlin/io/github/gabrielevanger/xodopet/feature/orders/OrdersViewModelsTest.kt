package io.github.gabrielevanger.xodopet.feature.orders

import androidx.lifecycle.SavedStateHandle
import io.github.gabrielevanger.xodopet.core.domain.DiagnosticLog
import io.github.gabrielevanger.xodopet.core.domain.model.OrderStatus
import io.github.gabrielevanger.xodopet.core.domain.orders.OrderError
import io.github.gabrielevanger.xodopet.core.domain.orders.ReorderToCart
import io.github.gabrielevanger.xodopet.core.domain.testing.FakeCartRepository
import io.github.gabrielevanger.xodopet.core.domain.testing.FakeOrderRepository
import io.github.gabrielevanger.xodopet.core.domain.testing.FakeStoreRepository
import io.github.gabrielevanger.xodopet.core.domain.testing.aProduct
import io.github.gabrielevanger.xodopet.core.domain.testing.aVariant
import io.github.gabrielevanger.xodopet.core.domain.testing.anOrderDetails
import io.github.gabrielevanger.xodopet.core.domain.testing.anOrderSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
class OrdersViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val orders = FakeOrderRepository()
    private val carts = FakeCartRepository()
    private val stores = FakeStoreRepository(
        products = listOf(aProduct(id = "golden", variants = listOf(aVariant(id = "1kg", stock = 5)))),
    )
    private val warnings = mutableListOf<String>()
    private val log = DiagnosticLog { message, _ -> warnings += message }

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun details() = OrderDetailsViewModel(
        SavedStateHandle(mapOf(ORDER_ID_ARG to "pedido-1")),
        orders,
        ReorderToCart(stores, carts),
        log,
    )

    private fun OrderDetailsViewModel.loaded() = assertInstanceOf(OrderDetailsUiState.Loaded::class.java, uiState.value)

    @Test
    fun `list loads, handles errors and sign in`() = runTest(dispatcher) {
        val viewModel = OrdersViewModel(orders, log)
        assertEquals(OrdersUiState.Loading, viewModel.uiState.value)

        viewModel.load()
        advanceUntilIdle()
        assertEquals(listOf(anOrderSummary()), (viewModel.uiState.value as OrdersUiState.Loaded).orders)

        orders.failWith = OrderError.NotSignedIn
        viewModel.load()
        advanceUntilIdle()
        assertEquals(OrdersUiState.NotSignedIn, viewModel.uiState.value)
        assertTrue(warnings.isEmpty())

        orders.failWith = OrderError.Network
        viewModel.load()
        advanceUntilIdle()
        assertEquals(OrdersUiState.Error, viewModel.uiState.value)
        assertEquals(1, warnings.size)
    }

    @Test
    fun `details load and follow live changes`() = runTest(dispatcher) {
        val viewModel = details()
        advanceUntilIdle()
        assertEquals(OrderStatus.Pending, viewModel.loaded().order.status)

        orders.details = mapOf("pedido-1" to anOrderDetails(status = OrderStatus.Accepted))
        orders.emitChange()
        advanceUntilIdle()

        assertEquals(OrderStatus.Accepted, viewModel.loaded().order.status)
    }

    @Test
    fun `not found, error and retry`() = runTest(dispatcher) {
        orders.details = emptyMap()
        val missing = details()
        advanceUntilIdle()
        assertEquals(OrderDetailsUiState.NotFound, missing.uiState.value)

        orders.details = mapOf("pedido-1" to anOrderDetails())
        orders.failWith = OrderError.Network
        val failing = details()
        advanceUntilIdle()
        assertEquals(OrderDetailsUiState.Error, failing.uiState.value)

        failing.retry()
        advanceUntilIdle()
        failing.loaded()
    }

    @Test
    fun `refresh failure keeps what is on screen`() = runTest(dispatcher) {
        val viewModel = details()
        advanceUntilIdle()

        orders.failWith = OrderError.Network
        orders.emitChange()
        advanceUntilIdle()

        assertEquals(OrderStatus.Pending, viewModel.loaded().order.status)
    }

    @Test
    fun `cancel asks first, then cancels`() = runTest(dispatcher) {
        val viewModel = details()
        advanceUntilIdle()

        viewModel.askToCancel()
        assertTrue(viewModel.loaded().confirmingCancel)
        viewModel.dismissCancel()
        assertFalse(viewModel.loaded().confirmingCancel)

        viewModel.askToCancel()
        viewModel.confirmCancel()
        advanceUntilIdle()

        assertEquals(listOf("pedido-1"), orders.canceled)
        assertEquals(OrderStatus.Canceled, viewModel.loaded().order.status)
        assertNull(viewModel.loaded().message)
    }

    @Test
    fun `cancel refused or failed`() = runTest(dispatcher) {
        val viewModel = details()
        advanceUntilIdle()

        orders.failWith = OrderError.CannotCancel
        viewModel.confirmCancel()
        advanceUntilIdle()
        assertEquals(OrderMessage.CannotCancel, viewModel.loaded().message)

        viewModel.messageShown()
        orders.failWith = OrderError.Network
        viewModel.confirmCancel()
        advanceUntilIdle()
        assertEquals(OrderMessage.ActionFailed, viewModel.loaded().message)
    }

    @Test
    fun `reorder opens the cart and tells what changed`() = runTest(dispatcher) {
        val viewModel = details()
        advanceUntilIdle()
        var cartOpened = false
        backgroundScope.launch {
            viewModel.openCart.first()
            cartOpened = true
        }

        viewModel.reorder()
        advanceUntilIdle()

        assertTrue(cartOpened)
        assertEquals(
            OrderMessage.ReorderSomeChanged,
            viewModel.loaded().message,
            "a GranPlus não existe nesta loja falsa",
        )
        assertEquals("1kg", carts.cart.value!!.lines.single().variantId)
    }

    @Test
    fun `reorder with nothing available and with the store gone`() = runTest(dispatcher) {
        stores.products = emptyList()
        val viewModel = details()
        advanceUntilIdle()

        viewModel.reorder()
        advanceUntilIdle()
        assertEquals(OrderMessage.ReorderNothing, viewModel.loaded().message)

        stores.stores = emptyMap()
        viewModel.reorder()
        advanceUntilIdle()
        assertEquals(OrderMessage.ActionFailed, viewModel.loaded().message)
    }
}
