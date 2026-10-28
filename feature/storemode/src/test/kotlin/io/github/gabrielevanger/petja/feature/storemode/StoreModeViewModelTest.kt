package io.github.gabrielevanger.petja.feature.storemode

import app.cash.turbine.test
import io.github.gabrielevanger.petja.core.domain.DiagnosticLog
import io.github.gabrielevanger.petja.core.domain.model.OrderStatus
import io.github.gabrielevanger.petja.core.domain.storemode.ManagedStore
import io.github.gabrielevanger.petja.core.domain.storemode.StoreAction
import io.github.gabrielevanger.petja.core.domain.storemode.StoreOrderError
import io.github.gabrielevanger.petja.core.domain.testing.FakeStoreOrderRepository
import io.github.gabrielevanger.petja.core.domain.testing.LAVRADO
import io.github.gabrielevanger.petja.core.domain.testing.aStoreOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
class StoreModeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeStoreOrderRepository()
    private var alerts = 0
    private val warnings = mutableListOf<String>()
    private val log = DiagnosticLog { message, _ -> warnings += message }

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    /** Como a tela faz: o ViewModel só carrega e liga o Realtime quando a tela manda OnStart. */
    private fun viewModel() = StoreModeViewModel(repository, {
        alerts++
    }, log).apply { onAction(StoreModeAction.OnStart) }

    private fun StoreModeViewModel.loaded() = assertInstanceOf(StoreModeUiState.Loaded::class.java, uiState.value)

    @Test
    fun `opens the first store without ringing for orders already there`() = runTest(dispatcher) {
        val viewModel = viewModel()
        assertEquals(StoreModeUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        assertEquals("Petshop Lavrado", viewModel.loaded().storeName)
        assertEquals(listOf("pedido-1"), viewModel.loaded().orders.map { it.id })
        assertEquals(0, alerts)
    }

    @Test
    fun `a new order arriving by realtime rings once`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        repository.orders = listOf(aStoreOrder(id = "pedido-2", number = 2)) + repository.orders
        repository.emitChange()
        advanceUntilIdle()

        assertEquals(1, alerts)
        assertEquals(listOf("pedido-2", "pedido-1"), viewModel.loaded().orders.map { it.id })

        repository.emitChange()
        advanceUntilIdle()
        assertEquals(1, alerts, "o mesmo pedido não toca de novo")
    }

    @Test
    fun `accepting moves the order and reloads`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(StoreModeAction.OnOrderAction(viewModel.loaded().orders.single(), StoreAction.Accept))
        // Trava os botões na hora do toque, antes de o servidor responder.
        assertEquals("pedido-1", viewModel.loaded().working)
        advanceUntilIdle()

        assertEquals(listOf(Triple("pedido-1", OrderStatus.Accepted, null)), repository.transitions)
        assertEquals(OrderStatus.Accepted, viewModel.loaded().orders.single().status)
        assertNull(viewModel.loaded().working)
    }

    @Test
    fun `rejecting asks for a reason first`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        val order = viewModel.loaded().orders.single()

        viewModel.onAction(StoreModeAction.OnOrderAction(order, StoreAction.Reject))
        assertEquals(ReasonRequest("pedido-1", 1, StoreAction.Reject), viewModel.loaded().askingReason)
        viewModel.onAction(StoreModeAction.OnDismissReason)
        assertNull(viewModel.loaded().askingReason)

        viewModel.onAction(StoreModeAction.OnOrderAction(order, StoreAction.Reject))
        viewModel.onAction(StoreModeAction.OnConfirmReason("   "))
        assertEquals(StoreAction.Reject, viewModel.loaded().askingReason?.action, "motivo em branco não vale")
        viewModel.onAction(StoreModeAction.OnConfirmReason("  Sem estoque "))
        advanceUntilIdle()

        assertNull(viewModel.loaded().askingReason)
        assertEquals(listOf(Triple("pedido-1", OrderStatus.Rejected, "Sem estoque")), repository.transitions)
    }

    @Test
    fun `refusals from the server become messages`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        val order = viewModel.loaded().orders.single()

        viewModel.events.test {
            repository.failWith = StoreOrderError.InvalidTransition
            viewModel.onAction(StoreModeAction.OnOrderAction(order, StoreAction.Accept))
            advanceUntilIdle()
            assertEquals(StoreModeEvent.ShowMessage(StoreMessage.OutOfDate), awaitItem())

            repository.failWith = StoreOrderError.NotAllowed
            viewModel.onAction(StoreModeAction.OnOrderAction(order, StoreAction.Accept))
            advanceUntilIdle()
            assertEquals(StoreModeEvent.ShowMessage(StoreMessage.NotAllowed), awaitItem())

            repository.failWith = StoreOrderError.Network
            viewModel.onAction(StoreModeAction.OnOrderAction(order, StoreAction.Accept))
            advanceUntilIdle()
            assertEquals(StoreModeEvent.ShowMessage(StoreMessage.ActionFailed), awaitItem())
        }
        assertEquals(listOf("Falha ao mudar a etapa do pedido"), warnings)
    }

    @Test
    fun `switching store listens to the new one`() = runTest(dispatcher) {
        repository.stores = listOf(LAVRADO, ManagedStore("store-2", "Agropet Caçari"))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(StoreModeAction.OnStoreClick("store-1"))
        viewModel.onAction(StoreModeAction.OnStoreClick("store-2"))
        advanceUntilIdle()

        assertEquals("store-2", viewModel.loaded().storeId)
        assertEquals(listOf("store-1", "store-2"), repository.ordersCalls)
    }

    @Test
    fun `no store, signed out, errors and retry`() = runTest(dispatcher) {
        repository.stores = emptyList()
        val noStore = viewModel()
        advanceUntilIdle()
        assertEquals(StoreModeUiState.NoStore, noStore.uiState.value)

        repository.failWith = StoreOrderError.NotSignedIn
        val signedOut = viewModel()
        advanceUntilIdle()
        assertEquals(StoreModeUiState.NotSignedIn, signedOut.uiState.value)

        repository.stores = listOf(LAVRADO)
        repository.failWith = StoreOrderError.Network
        val failing = viewModel()
        advanceUntilIdle()
        assertEquals(StoreModeUiState.Error, failing.uiState.value)

        failing.onAction(StoreModeAction.OnRetryClick)
        advanceUntilIdle()
        assertEquals("Petshop Lavrado", failing.loaded().storeName)
    }

    @Test
    fun `a failed refresh keeps the list on screen`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.events.test {
            repository.failWith = StoreOrderError.Network
            repository.emitChange()
            advanceUntilIdle()
            assertEquals(StoreModeEvent.ShowMessage(StoreMessage.ActionFailed), awaitItem())
        }
        assertEquals(1, viewModel.loaded().orders.size)
    }

    @Test
    fun `stop pauses realtime and an order that arrived meanwhile rings on return`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(StoreModeAction.OnStop)
        repository.orders = listOf(aStoreOrder(id = "pedido-2", number = 2)) + repository.orders
        repository.emitChange()
        advanceUntilIdle()
        assertEquals(1, viewModel.loaded().orders.size, "parada, a tela não ouve o Realtime")
        assertEquals(0, alerts)

        viewModel.onAction(StoreModeAction.OnStart)
        advanceUntilIdle()
        assertEquals(2, viewModel.loaded().orders.size)
        assertEquals(1, alerts, "o pedido que chegou enquanto a tela estava fora toca ao voltar")
    }

    @Test
    fun `back and sign in become events`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.events.test {
            viewModel.onAction(StoreModeAction.OnBackClick)
            assertEquals(StoreModeEvent.Close, awaitItem())
            viewModel.onAction(StoreModeAction.OnSignInClick)
            assertEquals(StoreModeEvent.OpenSignIn, awaitItem())
        }
    }
}
