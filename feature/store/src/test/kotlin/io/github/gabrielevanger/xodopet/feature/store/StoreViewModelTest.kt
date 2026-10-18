package io.github.gabrielevanger.xodopet.feature.store

import androidx.lifecycle.SavedStateHandle
import io.github.gabrielevanger.xodopet.core.domain.cart.CartActions
import io.github.gabrielevanger.xodopet.core.domain.model.Cart
import io.github.gabrielevanger.xodopet.core.domain.model.CartLine
import io.github.gabrielevanger.xodopet.core.domain.model.Money
import io.github.gabrielevanger.xodopet.core.domain.store.LoadStoreCatalog
import io.github.gabrielevanger.xodopet.core.domain.testing.FakeCartRepository
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

    private val cart = FakeCartRepository()

    private fun viewModel(storeId: String = "store-1") = StoreViewModel(
        SavedStateHandle(mapOf(STORE_ID_ARG to storeId)),
        LoadStoreCatalog(repository),
        CartActions(cart),
        cart,
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

    @Test
    fun `adding an option puts it in the cart, closes the sheet and confirms`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.openProduct("golden")

        viewModel.addToCart("15kg")
        advanceUntilIdle()

        assertEquals(listOf("15kg"), cart.cart.value?.lines?.map { it.variantId })
        assertNull((viewModel.uiState.value as StoreUiState.Loaded).selectedProduct)
        assertEquals(StoreCartMessage.Added("Ração Golden Adultos", "15 kg"), viewModel.cartState.value.message)

        viewModel.messageShown()
        assertNull(viewModel.cartState.value.message)
    }

    @Test
    fun `cart from another store asks before replacing`() = runTest(dispatcher) {
        cart.update { Cart("outra", "Casa do Bicho", Money(0), listOf(aCartLine())) }
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.addToCart("1kg")
        advanceUntilIdle()
        assertEquals(ReplaceCartPrompt("Casa do Bicho", "1kg"), viewModel.cartState.value.replacePrompt)
        assertEquals("outra", cart.cart.value?.storeId)

        viewModel.confirmReplaceCart()
        advanceUntilIdle()
        assertNull(viewModel.cartState.value.replacePrompt)
        assertEquals("store-1", cart.cart.value?.storeId)
    }

    @Test
    fun `keeping the other cart changes nothing`() = runTest(dispatcher) {
        cart.update { Cart("outra", "Casa do Bicho", Money(0), listOf(aCartLine())) }
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.addToCart("1kg")
        advanceUntilIdle()
        viewModel.dismissReplaceCart()

        assertNull(viewModel.cartState.value.replacePrompt)
        assertEquals("outra", cart.cart.value?.storeId)
    }

    private fun aCartLine() = CartLine("x", "p", "Bifinho", "65 g", Money(890), quantity = 1, maxQuantity = 5)
}
