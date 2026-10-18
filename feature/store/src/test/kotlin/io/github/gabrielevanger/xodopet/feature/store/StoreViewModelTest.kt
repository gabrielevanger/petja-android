package io.github.gabrielevanger.xodopet.feature.store

import androidx.lifecycle.SavedStateHandle
import io.github.gabrielevanger.xodopet.core.domain.store.LoadStoreCatalog
import io.github.gabrielevanger.xodopet.core.domain.testing.FakeStoreRepository
import io.github.gabrielevanger.xodopet.core.domain.testing.aProduct
import io.github.gabrielevanger.xodopet.core.domain.testing.aVariant
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StoreViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeStoreRepository(
        products = listOf(
            aProduct(
                id = "golden",
                variants = listOf(aVariant(id = "1kg", label = "1 kg"), aVariant(id = "15kg", label = "15 kg")),
            ),
        ),
    )
    private val warnings = mutableListOf<String>()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(storeId: String = "store-1") = StoreViewModel(
        SavedStateHandle(mapOf(STORE_ID_ARG to storeId)),
        LoadStoreCatalog(repository),
    ) { message, _ -> warnings += message }

    @Test
    fun `loads the store from the route argument`() = runTest(dispatcher) {
        val viewModel = viewModel()
        assertEquals(StoreUiState.Loading, viewModel.uiState.value)

        advanceUntilIdle()

        val state = assertInstanceOf(StoreUiState.Loaded::class.java, viewModel.uiState.value)
        assertEquals("Petshop Lavrado", state.header.name)
        assertEquals(listOf("Ração"), state.sections.map { it.title })
    }

    @Test
    fun `unknown store shows not found`() = runTest(dispatcher) {
        val viewModel = viewModel(storeId = "sumiu")
        advanceUntilIdle()

        assertEquals(StoreUiState.NotFound, viewModel.uiState.value)
        assertEquals(1, warnings.size)
    }

    @Test
    fun `network error shows error, logs it, and retry recovers`() = runTest(dispatcher) {
        repository.failWith = IOException("sem rede")
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(StoreUiState.Error, viewModel.uiState.value)
        assertEquals(1, warnings.size)

        viewModel.retry()
        advanceUntilIdle()

        assertInstanceOf(StoreUiState.Loaded::class.java, viewModel.uiState.value)
    }

    @Test
    fun `opens and closes a product`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.openProduct("golden")
        val opened = assertInstanceOf(StoreUiState.Loaded::class.java, viewModel.uiState.value)
        assertEquals(listOf("1 kg", "15 kg"), opened.selectedProduct?.variants?.map { it.label })

        viewModel.closeProduct()
        assertNull((viewModel.uiState.value as StoreUiState.Loaded).selectedProduct)
    }

    @Test
    fun `opening an unknown product does nothing`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.openProduct("nao-existe")

        assertNull((viewModel.uiState.value as StoreUiState.Loaded).selectedProduct)
    }
}
