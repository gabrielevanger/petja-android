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
import io.github.gabrielevanger.xodopet.core.ui.toCartBar
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class StoreViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val loadStoreCatalog: LoadStoreCatalog,
    private val cartActions: CartActions,
    private val cartRepository: CartRepository,
    private val log: DiagnosticLog,
) : ViewModel() {

    private val storeId: String = requireNotNull(savedStateHandle[STORE_ID_ARG]) { "Rota da loja sem $STORE_ID_ARG" }

    private val _uiState = MutableStateFlow(StoreUiState())
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    private val _events = Channel<StoreEvent>(Channel.BUFFERED)
    val events: Flow<StoreEvent> = _events.receiveAsFlow()

    /** Catálogo carregado, para abrir o detalhe de um produto sem nova chamada. */
    private var catalog: StoreCatalog? = null
    private var started = false
    private var loading: Job? = null
    private var listeningCart: Job? = null

    fun onAction(action: StoreAction) {
        when (action) {
            StoreAction.OnStart -> onStart()
            StoreAction.OnStop -> listeningCart?.cancel()
            StoreAction.OnBackClick -> send(StoreEvent.Close)
            StoreAction.OnRetryClick -> load()
            is StoreAction.OnProductClick -> openProduct(action.productId)
            StoreAction.OnProductDismiss -> closeProduct()
            is StoreAction.OnAddToCart -> addToCart(action.variantId, replaceOtherStore = false)
            StoreAction.OnConfirmReplaceCart -> confirmReplaceCart()
            StoreAction.OnDismissReplaceCart -> _uiState.update { it.copy(replacePrompt = null) }
            StoreAction.OnCartClick -> send(StoreEvent.OpenCart)
        }
    }

    private fun onStart() {
        listeningCart?.cancel()
        listeningCart = cartRepository.cart
            .onEach { cart -> _uiState.update { it.copy(cartBar = cart.toCartBar()) } }
            .catch { log.warn("Falha ao ler o carrinho", it) }
            .launchIn(viewModelScope)
        if (started) return
        started = true
        load()
    }

    private fun load() {
        loading?.cancel()
        loading = flow { emit(loadStoreCatalog(storeId)) }
            .onStart { updateContent { StoreContent.Loading } }
            .onEach { loaded ->
                catalog = loaded
                updateContent { loaded.toContent() }
            }
            .catch { error ->
                val content = if (error is StoreNotFoundException) {
                    log.warn("Loja $storeId não encontrada", error)
                    StoreContent.NotFound
                } else {
                    log.warn("Falha ao carregar a loja $storeId", error)
                    StoreContent.Error
                }
                updateContent { content }
            }
            .launchIn(viewModelScope)
    }

    private fun openProduct(productId: String) {
        val product = catalog?.sections?.flatMap { it.products }?.firstOrNull { it.id == productId } ?: return
        updateContent { content ->
            if (content is StoreContent.Loaded) content.copy(selectedProduct = product.toDetail()) else content
        }
    }

    private fun closeProduct() = updateContent { content ->
        if (content is StoreContent.Loaded) content.copy(selectedProduct = null) else content
    }

    /** Adiciona uma unidade da opção. Com carrinho de outra loja, só troca depois de o cliente confirmar. */
    private fun addToCart(variantId: String, replaceOtherStore: Boolean) {
        val catalog = catalog ?: return
        val product =
            catalog.sections.flatMap { it.products }.firstOrNull { p -> p.variants.any { it.id == variantId } }
                ?: return
        val variant = product.variants.first { it.id == variantId }
        viewModelScope.launch {
            val result = cartActions.add(catalog.store, product, variant, replaceOtherStore)
            closeProduct()
            when (result) {
                AddToCartResult.Added -> send(
                    StoreEvent.ShowMessage(StoreCartMessage.Added(product.name, variant.label)),
                )

                is AddToCartResult.OtherStoreInCart ->
                    _uiState.update { it.copy(replacePrompt = ReplaceCartPrompt(result.storeName, variantId)) }

                AddToCartResult.LimitReached -> send(StoreEvent.ShowMessage(StoreCartMessage.LimitReached))

                AddToCartResult.Unavailable -> send(StoreEvent.ShowMessage(StoreCartMessage.Unavailable))
            }
        }
    }

    private fun confirmReplaceCart() {
        val prompt = _uiState.value.replacePrompt ?: return
        _uiState.update { it.copy(replacePrompt = null) }
        addToCart(prompt.variantId, replaceOtherStore = true)
    }

    private fun updateContent(change: (StoreContent) -> StoreContent) =
        _uiState.update { it.copy(content = change(it.content)) }

    private fun send(event: StoreEvent) {
        _events.trySend(event)
    }
}
