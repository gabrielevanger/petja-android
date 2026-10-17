package io.github.gabrielevanger.xodopet.feature.storefront

import app.cash.turbine.test
import io.github.gabrielevanger.xodopet.core.domain.storefront.LoadStorefront
import io.github.gabrielevanger.xodopet.core.domain.testing.FakeStorefrontRepository
import io.github.gabrielevanger.xodopet.core.domain.testing.aStore
import io.github.gabrielevanger.xodopet.core.domain.testing.anExternalStore
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

    private fun viewModel() = StorefrontViewModel(LoadStorefront(repository))

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
            viewModel.selectPlace(satelite)
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

        viewModel.selectPlace(BoaVistaSamplePlaces.first())
        advanceUntilIdle()

        assertEquals(1, repository.storesNearCalls.size)
    }

    @Test
    fun `network error shows error and retry recovers`() = runTest(dispatcher) {
        repository.failWith = IOException("sem rede")
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(StorefrontContent.Error, viewModel.uiState.value.content)

        viewModel.retry()
        advanceUntilIdle()

        assertInstanceOf(StorefrontContent.Loaded::class.java, viewModel.uiState.value.content)
    }

    @Test
    fun `switching place mid load keeps only the latest answer`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val buritis = BoaVistaSamplePlaces.first { it.name == "Buritis" }

        viewModel.selectPlace(buritis)
        advanceUntilIdle()

        assertEquals(buritis, viewModel.uiState.value.selectedPlace)
        assertInstanceOf(StorefrontContent.Loaded::class.java, viewModel.uiState.value.content)
        assertEquals(buritis.point, repository.storesNearCalls.last())
    }
}
