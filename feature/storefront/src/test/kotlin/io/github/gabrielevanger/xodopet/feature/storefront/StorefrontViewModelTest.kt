package io.github.gabrielevanger.xodopet.feature.storefront

import app.cash.turbine.test
import io.github.gabrielevanger.xodopet.core.domain.model.Cart
import io.github.gabrielevanger.xodopet.core.domain.model.CartLine
import io.github.gabrielevanger.xodopet.core.domain.model.Money
import io.github.gabrielevanger.xodopet.core.domain.storefront.LoadStorefront
import io.github.gabrielevanger.xodopet.core.domain.testing.FakeCartRepository
import io.github.gabrielevanger.xodopet.core.domain.testing.FakeDeliveryPlaceRepository
import io.github.gabrielevanger.xodopet.core.domain.testing.FakeStorefrontRepository
import io.github.gabrielevanger.xodopet.core.domain.testing.aStore
import io.github.gabrielevanger.xodopet.core.domain.testing.anExternalStore
import io.github.gabrielevanger.xodopet.core.ui.CartBarUi
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
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StorefrontViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeStorefrontRepository(stores = listOf(aStore()))

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private val warnings = mutableListOf<Pair<String, Throwable?>>()

    private val places = FakeDeliveryPlaceRepository()
    private val cart = FakeCartRepository()

    /** Como a tela faz: o ViewModel só começa a carregar quando a tela manda OnStart. */
    private fun viewModel() = StorefrontViewModel(LoadStorefront(repository), places, cart) { message, cause ->
        warnings += message to cause
    }.apply { onAction(StorefrontAction.OnStart) }

    @Test
    fun `loads the first place on start`() = runTest(dispatcher) {
        val viewModel = viewModel()
        assertEquals(StorefrontContent.Loading, viewModel.uiState.value.content)

        advanceUntilIdle()

        val content = assertInstanceOf(StorefrontContent.Loaded::class.java, viewModel.uiState.value.content)
        assertEquals(listOf("Petshop Lavrado"), content.partners.map { it.name })
        assertEquals(listOf(BoaVistaSamplePlaces.first().point), repository.storesNearCalls)
    }

    @Test
    fun `selecting a place loads it`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        val satelite = BoaVistaSamplePlaces.last()
        repository.stores = emptyList()
        repository.externalStores = listOf(anExternalStore())

        viewModel.uiState.test {
            skipItems(1)
            viewModel.onAction(StorefrontAction.OnPlaceClick(satelite))
            assertEquals(StorefrontContent.Loading, awaitItem().content)
            val loaded = awaitItem()
            assertEquals(satelite, loaded.selectedPlace)
            val content = assertInstanceOf(StorefrontContent.Loaded::class.java, loaded.content)
            assertEquals(true, content.noPartnerDelivers)
            assertEquals(listOf("Agropet Satélite"), content.nonPartners.map { it.name })
        }
    }

    @Test
    fun `selecting the same place again does not reload`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(StorefrontAction.OnPlaceClick(BoaVistaSamplePlaces.first()))
        advanceUntilIdle()

        assertEquals(1, repository.storesNearCalls.size)
    }

    @Test
    fun `network error shows error and retry recovers`() = runTest(dispatcher) {
        repository.failWith = IOException("sem rede")
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(StorefrontContent.Error, viewModel.uiState.value.content)
        assertEquals("sem rede", warnings.single().second?.message, "a causa vai para o log")

        viewModel.onAction(StorefrontAction.OnRetryClick)
        advanceUntilIdle()

        assertInstanceOf(StorefrontContent.Loaded::class.java, viewModel.uiState.value.content)
    }

    @Test
    fun `switching place mid load keeps only the latest answer`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val buritis = BoaVistaSamplePlaces.first { it.name == "Buritis" }

        viewModel.onAction(StorefrontAction.OnPlaceClick(buritis))
        advanceUntilIdle()

        assertEquals(buritis, viewModel.uiState.value.selectedPlace)
        assertInstanceOf(StorefrontContent.Loaded::class.java, viewModel.uiState.value.content)
        assertEquals(buritis.point, repository.storesNearCalls.last())
    }

    @Test
    fun `remembers the chosen place`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        val buritis = BoaVistaSamplePlaces.first { it.name == "Buritis" }

        viewModel.onAction(StorefrontAction.OnPlaceClick(buritis))
        advanceUntilIdle()
        assertEquals("Buritis", places.place.value?.name)

        val reopened = viewModel()
        advanceUntilIdle()
        assertEquals(buritis, reopened.uiState.value.selectedPlace)
        assertEquals(buritis.point, repository.storesNearCalls.last())
    }

    @Test
    fun `shows the cart bar when the cart has items`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(null, viewModel.uiState.value.cartBar)

        val line = CartLine("v", "p", "Ração", "1 kg", Money(3090), quantity = 2, maxQuantity = 5)
        cart.update { Cart("store-1", "Petshop Lavrado", Money(3000), listOf(line)) }
        advanceUntilIdle()

        assertEquals(CartBarUi("Petshop Lavrado", 2, "R$ 61,80"), viewModel.uiState.value.cartBar)
    }

    @Test
    fun `clicks become navigation events`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onAction(StorefrontAction.OnStoreClick("store-1"))
            assertEquals(StorefrontEvent.OpenStore("store-1"), awaitItem())
            viewModel.onAction(StorefrontAction.OnCartClick)
            assertEquals(StorefrontEvent.OpenCart, awaitItem())
            viewModel.onAction(StorefrontAction.OnAccountClick)
            assertEquals(StorefrontEvent.OpenAccount, awaitItem())
        }
    }

    @Test
    fun `starting again does not reload and stop pauses the cart`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(StorefrontAction.OnStop)
        val line = CartLine("v", "p", "Ração", "1 kg", Money(3090), quantity = 1, maxQuantity = 5)
        cart.update { Cart("store-1", "Petshop Lavrado", Money(3000), listOf(line)) }
        advanceUntilIdle()
        assertEquals(null, viewModel.uiState.value.cartBar, "parada, a tela não ouve o carrinho")

        viewModel.onAction(StorefrontAction.OnStart)
        advanceUntilIdle()
        assertEquals("Petshop Lavrado", viewModel.uiState.value.cartBar?.storeName)
        assertEquals(1, repository.storesNearCalls.size, "voltar para a tela não busca a vitrine de novo")
    }
}
