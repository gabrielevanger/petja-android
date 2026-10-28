package io.github.gabrielevanger.petja.feature.orders.details

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.github.gabrielevanger.petja.core.domain.DiagnosticLog
import io.github.gabrielevanger.petja.core.domain.model.OrderStatus
import io.github.gabrielevanger.petja.core.domain.orders.OrderError
import io.github.gabrielevanger.petja.core.domain.orders.ReorderToCart
import io.github.gabrielevanger.petja.core.domain.testing.FakeCartRepository
import io.github.gabrielevanger.petja.core.domain.testing.FakeOrderRepository
import io.github.gabrielevanger.petja.core.domain.testing.FakeStoreRepository
import io.github.gabrielevanger.petja.core.domain.testing.aProduct
import io.github.gabrielevanger.petja.core.domain.testing.aVariant
import io.github.gabrielevanger.petja.core.domain.testing.anOrderDetails
import io.github.gabrielevanger.petja.feature.orders.ORDER_ID_ARG
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrderDetailsViewModelTest {
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

    /** Como a tela faz: o ViewModel só carrega e liga o status ao vivo quando a tela manda OnStart. */
    private fun details() = OrderDetailsViewModel(
        SavedStateHandle(mapOf(ORDER_ID_ARG to "pedido-1")),
        orders,
        ReorderToCart(stores, carts),
        log,
    ).apply { onAction(OrderDetailsAction.OnStart) }

    private fun OrderDetailsViewModel.loaded() = assertInstanceOf(OrderDetailsUiState.Loaded::class.java, uiState.value)

    @Test
    fun `loads and follows live changes until the screen stops`() = runTest(dispatcher) {
        val viewModel = details()
        advanceUntilIdle()
        assertEquals(OrderStatus.Pending, viewModel.loaded().order.status)

        orders.details = mapOf("pedido-1" to anOrderDetails(status = OrderStatus.Accepted))
        orders.emitChange()
        advanceUntilIdle()
        assertEquals(OrderStatus.Accepted, viewModel.loaded().order.status)

        viewModel.onAction(OrderDetailsAction.OnStop)
        orders.details = mapOf("pedido-1" to anOrderDetails(status = OrderStatus.Preparing))
        orders.emitChange()
        advanceUntilIdle()
        assertEquals(OrderStatus.Accepted, viewModel.loaded().order.status, "parada, a tela não ouve o Realtime")

        viewModel.onAction(OrderDetailsAction.OnStart)
        advanceUntilIdle()
        assertEquals(OrderStatus.Preparing, viewModel.loaded().order.status, "ao voltar, recarrega o que mudou")
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

        failing.onAction(OrderDetailsAction.OnRetryClick)
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

        viewModel.onAction(OrderDetailsAction.OnCancelClick)
        assertTrue(viewModel.loaded().confirmingCancel)
        viewModel.onAction(OrderDetailsAction.OnDismissCancel)
        assertFalse(viewModel.loaded().confirmingCancel)

        viewModel.onAction(OrderDetailsAction.OnCancelClick)
        viewModel.onAction(OrderDetailsAction.OnConfirmCancel)
        advanceUntilIdle()

        assertEquals(listOf("pedido-1"), orders.canceled)
        assertEquals(OrderStatus.Canceled, viewModel.loaded().order.status)
        assertFalse(viewModel.loaded().working)
    }

    @Test
    fun `cancel refused or failed`() = runTest(dispatcher) {
        val viewModel = details()
        advanceUntilIdle()

        viewModel.events.test {
            orders.failWith = OrderError.CannotCancel
            viewModel.onAction(OrderDetailsAction.OnConfirmCancel)
            advanceUntilIdle()
            assertEquals(OrderDetailsEvent.ShowMessage(OrderMessage.CannotCancel), awaitItem())

            orders.failWith = OrderError.Network
            viewModel.onAction(OrderDetailsAction.OnConfirmCancel)
            advanceUntilIdle()
            assertEquals(OrderDetailsEvent.ShowMessage(OrderMessage.ActionFailed), awaitItem())
        }
    }

    @Test
    fun `reorder opens the cart and tells what changed`() = runTest(dispatcher) {
        val viewModel = details()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onAction(OrderDetailsAction.OnReorderClick)
            advanceUntilIdle()
            assertEquals(OrderDetailsEvent.OpenCart, awaitItem())
            assertEquals(
                OrderDetailsEvent.ShowMessage(OrderMessage.ReorderSomeChanged),
                awaitItem(),
                "a GranPlus não existe nesta loja falsa",
            )
        }
        assertEquals("1kg", carts.cart.value!!.lines.single().variantId)
    }

    @Test
    fun `reorder with nothing available and with the store gone`() = runTest(dispatcher) {
        stores.products = emptyList()
        val viewModel = details()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onAction(OrderDetailsAction.OnReorderClick)
            advanceUntilIdle()
            assertEquals(OrderDetailsEvent.ShowMessage(OrderMessage.ReorderNothing), awaitItem())

            stores.stores = emptyMap()
            viewModel.onAction(OrderDetailsAction.OnReorderClick)
            advanceUntilIdle()
            assertEquals(OrderDetailsEvent.ShowMessage(OrderMessage.ActionFailed), awaitItem())
        }
    }

    @Test
    fun `back and whatsapp become events`() = runTest(dispatcher) {
        val viewModel = details()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onAction(OrderDetailsAction.OnBackClick)
            assertEquals(OrderDetailsEvent.Close, awaitItem())
            viewModel.onAction(OrderDetailsAction.OnWhatsappClick("5595900000001"))
            assertTrue((awaitItem() as OrderDetailsEvent.OpenLink).uri.contains("5595900000001"))
        }
    }
}
