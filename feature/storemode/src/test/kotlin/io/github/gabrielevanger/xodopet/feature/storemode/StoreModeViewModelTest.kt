package io.github.gabrielevanger.xodopet.feature.storemode

import io.github.gabrielevanger.xodopet.core.domain.DiagnosticLog
import io.github.gabrielevanger.xodopet.core.domain.model.OrderStatus
import io.github.gabrielevanger.xodopet.core.domain.storemode.ManagedStore
import io.github.gabrielevanger.xodopet.core.domain.storemode.StoreAction
import io.github.gabrielevanger.xodopet.core.domain.storemode.StoreOrderError
import io.github.gabrielevanger.xodopet.core.domain.testing.FakeStoreOrderRepository
import io.github.gabrielevanger.xodopet.core.domain.testing.LAVRADO
import io.github.gabrielevanger.xodopet.core.domain.testing.aStoreOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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

    private fun viewModel() = StoreModeViewModel(repository, { alerts++ }, log)

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

        viewModel.act(viewModel.loaded().orders.single(), StoreAction.Accept)
        runCurrent()
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

        viewModel.act(order, StoreAction.Reject)
        assertEquals(ReasonRequest("pedido-1", 1, StoreAction.Reject), viewModel.loaded().askingReason)
        viewModel.dismissReason()
        assertNull(viewModel.loaded().askingReason)

        viewModel.act(order, StoreAction.Reject)
        viewModel.confirmReason("   ")
        assertEquals(StoreAction.Reject, viewModel.loaded().askingReason?.action, "motivo em branco não vale")
        viewModel.confirmReason("  Sem estoque ")
        advanceUntilIdle()

        assertNull(viewModel.loaded().askingReason)
        assertEquals(listOf(Triple("pedido-1", OrderStatus.Rejected, "Sem estoque")), repository.transitions)
    }

    @Test
    fun `refusals from the server become messages`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        val order = viewModel.loaded().orders.single()

        repository.failWith = StoreOrderError.InvalidTransition
        viewModel.act(order, StoreAction.Accept)
        advanceUntilIdle()
        assertEquals(StoreMessage.OutOfDate, viewModel.loaded().message)
        viewModel.messageShown()
        assertNull(viewModel.loaded().message)

        repository.failWith = StoreOrderError.NotAllowed
        viewModel.act(order, StoreAction.Accept)
        advanceUntilIdle()
        assertEquals(StoreMessage.NotAllowed, viewModel.loaded().message)

        repository.failWith = StoreOrderError.Network
        viewModel.act(order, StoreAction.Accept)
        advanceUntilIdle()
        assertEquals(StoreMessage.ActionFailed, viewModel.loaded().message)
        assertEquals(listOf("Falha ao mudar a etapa do pedido"), warnings)
    }

    @Test
    fun `switching store listens to the new one`() = runTest(dispatcher) {
        repository.stores = listOf(LAVRADO, ManagedStore("store-2", "Agropet Caçari"))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.selectStore("store-1")
        viewModel.selectStore("store-2")
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

        failing.retry()
        advanceUntilIdle()
        assertEquals("Petshop Lavrado", failing.loaded().storeName)
    }

    @Test
    fun `a failed refresh keeps the list on screen`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        repository.failWith = StoreOrderError.Network
        repository.emitChange()
        advanceUntilIdle()

        assertEquals(1, viewModel.loaded().orders.size)
        assertEquals(StoreMessage.ActionFailed, viewModel.loaded().message)
    }
}
