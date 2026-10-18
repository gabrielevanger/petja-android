package io.github.gabrielevanger.xodopet.feature.store

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gabrielevanger.xodopet.core.domain.DiagnosticLog
import io.github.gabrielevanger.xodopet.core.domain.cart.AddToCartResult
import io.github.gabrielevanger.xodopet.core.domain.cart.CartActions
import io.github.gabrielevanger.xodopet.core.domain.cart.CartRepository
import io.github.gabrielevanger.xodopet.core.domain.store.LoadStoreCatalog
import io.github.gabrielevanger.xodopet.core.domain.store.StoreCatalog
import io.github.gabrielevanger.xodopet.core.domain.store.StoreNotFoundException
import io.github.gabrielevanger.xodopet.core.ui.CartBarUi
import io.github.gabrielevanger.xodopet.core.ui.toCartBar
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class StoreViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val loadStoreCatalog: LoadStoreCatalog,
    private val cartActions: CartActions,
    cartRepository: CartRepository,
    private val log: DiagnosticLog,
) : ViewModel() {

    private val storeId: String = requireNotNull(savedStateHandle[STORE_ID_ARG]) { "Rota da loja sem $STORE_ID_ARG" }

    private val _uiState = MutableStateFlow<StoreUiState>(StoreUiState.Loading)
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    private val _cartState = MutableStateFlow(StoreCartState())
    val cartState: StateFlow<StoreCartState> = _cartState.asStateFlow()

    val cartBar: StateFlow<CartBarUi?> = cartRepository.cart
        .map { it.toCartBar() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** Catálogo carregado, para abrir o detalhe de um produto sem nova chamada. */
    private var catalog: StoreCatalog? = null
    private var loading: Job? = null

    init {
        load()
    }

    fun retry() = load()

    fun openProduct(productId: String) {
        val product = catalog?.sections?.flatMap { it.products }?.firstOrNull { it.id == productId } ?: return
        _uiState.update { state ->
            if (state is StoreUiState.Loaded) state.copy(selectedProduct = product.toDetail()) else state
        }
    }

    /** Adiciona uma unidade da opção. Com carrinho de outra loja, só troca depois de o cliente confirmar. */
    fun addToCart(variantId: String, replaceOtherStore: Boolean = false) {
        val catalog = catalog ?: return
        val product =
            catalog.sections.flatMap { it.products }.firstOrNull { p -> p.variants.any { it.id == variantId } }
                ?: return
        val variant = product.variants.first { it.id == variantId }
        viewModelScope.launch {
            val result = cartActions.add(catalog.store, product, variant, replaceOtherStore)
            closeProduct()
            _cartState.update {
                when (result) {
                    AddToCartResult.Added -> it.copy(
                        replacePrompt = null,
                        message = StoreCartMessage.Added(product.name, variant.label),
                    )

                    is AddToCartResult.OtherStoreInCart -> it.copy(
                        replacePrompt = ReplaceCartPrompt(result.storeName, variantId),
                    )

                    AddToCartResult.LimitReached -> it.copy(message = StoreCartMessage.LimitReached)

                    AddToCartResult.Unavailable -> it.copy(message = StoreCartMessage.Unavailable)
                }
            }
        }
    }

    fun confirmReplaceCart() {
        val prompt = _cartState.value.replacePrompt ?: return
        _cartState.update { it.copy(replacePrompt = null) }
        addToCart(prompt.variantId, replaceOtherStore = true)
    }

    fun dismissReplaceCart() = _cartState.update { it.copy(replacePrompt = null) }

    fun messageShown() = _cartState.update { it.copy(message = null) }

    fun closeProduct() {
        _uiState.update { state -> if (state is StoreUiState.Loaded) state.copy(selectedProduct = null) else state }
    }

    private fun load() {
        loading?.cancel()
        _uiState.value = StoreUiState.Loading
        loading = viewModelScope.launch {
            _uiState.value = try {
                loadStoreCatalog(storeId).also { catalog = it }.toUiState()
            } catch (e: CancellationException) {
                throw e
            } catch (e: StoreNotFoundException) {
                log.warn("Loja $storeId não encontrada", e)
                StoreUiState.NotFound
            } catch (e: Exception) {
                log.warn("Falha ao carregar a loja $storeId", e)
                StoreUiState.Error
            }
        }
    }
}

/** Mantém o resumo do carrinho vivo por alguns segundos sem tela, para sobreviver à rotação. */
private const val STOP_TIMEOUT_MILLIS = 5_000L
