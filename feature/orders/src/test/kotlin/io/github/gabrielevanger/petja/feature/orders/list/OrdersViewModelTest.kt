package io.github.gabrielevanger.petja.feature.orders.list

import app.cash.turbine.test
import io.github.gabrielevanger.petja.core.domain.DiagnosticLog
import io.github.gabrielevanger.petja.core.domain.orders.OrderError
import io.github.gabrielevanger.petja.core.domain.testing.FakeOrderRepository
import io.github.gabrielevanger.petja.core.domain.testing.anOrderSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val orders = FakeOrderRepository()
    private val warnings = mutableListOf<String>()
    private val log = DiagnosticLog { message, _ -> warnings += message }

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loads on start, handles errors and sign in`() = runTest(dispatcher) {
        val viewModel = OrdersViewModel(orders, log)
        assertEquals(OrdersUiState.Loading, viewModel.uiState.value)

        viewModel.onAction(OrdersAction.OnStart)
        advanceUntilIdle()
        assertEquals(listOf(anOrderSummary()), (viewModel.uiState.value as OrdersUiState.Loaded).orders)

        orders.failWith = OrderError.NotSignedIn
        viewModel.onAction(OrdersAction.OnStart)
        advanceUntilIdle()
        assertEquals(OrdersUiState.NotSignedIn, viewModel.uiState.value)
        assertTrue(warnings.isEmpty())

        orders.failWith = OrderError.Network
        viewModel.onAction(OrdersAction.OnRetryClick)
        advanceUntilIdle()
        assertEquals(OrdersUiState.Error, viewModel.uiState.value)
        assertEquals(1, warnings.size)
    }

    @Test
    fun `clicks become navigation events`() = runTest(dispatcher) {
        val viewModel = OrdersViewModel(orders, log)

        viewModel.events.test {
            viewModel.onAction(OrdersAction.OnOrderClick("pedido-1"))
            assertEquals(OrdersEvent.OpenOrder("pedido-1"), awaitItem())
            viewModel.onAction(OrdersAction.OnSignInClick)
            assertEquals(OrdersEvent.OpenSignIn, awaitItem())
            viewModel.onAction(OrdersAction.OnBackClick)
            assertEquals(OrdersEvent.Close, awaitItem())
        }
    }
}
