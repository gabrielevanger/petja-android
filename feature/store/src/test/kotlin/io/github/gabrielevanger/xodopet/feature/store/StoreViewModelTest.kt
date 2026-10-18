package io.github.gabrielevanger.xodopet.feature.store

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
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

    /** Como a tela faz: o ViewModel só carrega quando a tela manda OnStart. */
    private fun viewModel(storeId: String = "store-1") = StoreViewModel(
        SavedStateHandle(mapOf(STORE_ID_ARG to storeId)),
        LoadStoreCatalog(repository),
        CartActions(cart),
        cart,
    ) { message, _ -> warnings += message }.apply { onAction(StoreAction.OnStart) }

    private fun StoreViewModel.content() = uiState.value.content

    @Test
    fun `loads the store from the route argument`() = runTest(dispatcher) {
        val viewModel = viewModel()
        assertEquals(StoreContent.Loading, viewModel.content())

        advanceUntilIdle()

        val state = assertInstanceOf(StoreContent.Loaded::class.java, viewModel.content())
        assertEquals("Petshop Lavrado", state.header.name)
        assertEquals(listOf("Ração"), state.sections.map { it.title })
    }

    @Test
    fun `unknown store shows not found`() = runTest(dispatcher) {
        val viewModel = viewModel(storeId = "sumiu")
        advanceUntilIdle()

        assertEquals(StoreContent.NotFound, viewModel.content())
        assertEquals(1, warnings.size)
    }

    @Test
    fun `network error shows error, logs it, and retry recovers`() = runTest(dispatcher) {
        repository.failWith = IOException("sem rede")
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(StoreContent.Error, viewModel.content())
        assertEquals(1, warnings.size)

        viewModel.onAction(StoreAction.OnRetryClick)
        advanceUntilIdle()

        assertInstanceOf(StoreContent.Loaded::class.java, viewModel.content())
    }

    @Test
    fun `opens and closes a product`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(StoreAction.OnProductClick("golden"))
        val opened = assertInstanceOf(StoreContent.Loaded::class.java, viewModel.content())
        assertEquals(listOf("1 kg", "15 kg"), opened.selectedProduct?.variants?.map { it.label })

        viewModel.onAction(StoreAction.OnProductDismiss)
        assertNull((viewModel.content() as StoreContent.Loaded).selectedProduct)
    }

    @Test
    fun `opening an unknown product does nothing`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(StoreAction.OnProductClick("nao-existe"))

        assertNull((viewModel.content() as StoreContent.Loaded).selectedProduct)
    }

    @Test
    fun `adding an option puts it in the cart, closes the sheet and confirms`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onAction(StoreAction.OnProductClick("golden"))

        viewModel.events.test {
            viewModel.onAction(StoreAction.OnAddToCart("15kg"))
            advanceUntilIdle()
            assertEquals(StoreEvent.ShowMessage(StoreCartMessage.Added("Ração Golden Adultos", "15 kg")), awaitItem())
        }
        assertEquals(listOf("15kg"), cart.cart.value?.lines?.map { it.variantId })
        assertNull((viewModel.content() as StoreContent.Loaded).selectedProduct)
        assertEquals("Petshop Lavrado", viewModel.uiState.value.cartBar?.storeName, "a barra do carrinho acompanha")
    }

    @Test
    fun `back and cart clicks become events, and stop pauses the cart`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onAction(StoreAction.OnBackClick)
            assertEquals(StoreEvent.Close, awaitItem())
            viewModel.onAction(StoreAction.OnCartClick)
            assertEquals(StoreEvent.OpenCart, awaitItem())
        }

        viewModel.onAction(StoreAction.OnStop)
        cart.update { Cart("outra", "Casa do Bicho", Money(0), listOf(aCartLine())) }
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.cartBar, "parada, a tela não ouve o carrinho")

        viewModel.onAction(StoreAction.OnStart)
        advanceUntilIdle()
        assertEquals("Casa do Bicho", viewModel.uiState.value.cartBar?.storeName)
        assertEquals(1, repository.catalogCalls, "voltar para a tela não carrega o catálogo de novo")
    }

    @Test
    fun `cart from another store asks before replacing`() = runTest(dispatcher) {
        cart.update { Cart("outra", "Casa do Bicho", Money(0), listOf(aCartLine())) }
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(StoreAction.OnAddToCart("1kg"))
        advanceUntilIdle()
        assertEquals(ReplaceCartPrompt("Casa do Bicho", "1kg"), viewModel.uiState.value.replacePrompt)
        assertEquals("outra", cart.cart.value?.storeId)

        viewModel.onAction(StoreAction.OnConfirmReplaceCart)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.replacePrompt)
        assertEquals("store-1", cart.cart.value?.storeId)
    }

    @Test
    fun `keeping the other cart changes nothing`() = runTest(dispatcher) {
        cart.update { Cart("outra", "Casa do Bicho", Money(0), listOf(aCartLine())) }
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onAction(StoreAction.OnAddToCart("1kg"))
        advanceUntilIdle()
        viewModel.onAction(StoreAction.OnDismissReplaceCart)

        assertNull(viewModel.uiState.value.replacePrompt)
        assertEquals("outra", cart.cart.value?.storeId)
    }

    private fun aCartLine() = CartLine("x", "p", "Bifinho", "65 g", Money(890), quantity = 1, maxQuantity = 5)
}
