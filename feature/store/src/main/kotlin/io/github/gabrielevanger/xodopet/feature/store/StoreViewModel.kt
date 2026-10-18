package io.github.gabrielevanger.xodopet.feature.store

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gabrielevanger.xodopet.core.domain.DiagnosticLog
import io.github.gabrielevanger.xodopet.core.domain.store.LoadStoreCatalog
import io.github.gabrielevanger.xodopet.core.domain.store.StoreCatalog
import io.github.gabrielevanger.xodopet.core.domain.store.StoreNotFoundException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class StoreViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val loadStoreCatalog: LoadStoreCatalog,
    private val log: DiagnosticLog,
) : ViewModel() {

    private val storeId: String = requireNotNull(savedStateHandle[STORE_ID_ARG]) { "Rota da loja sem $STORE_ID_ARG" }

    private val _uiState = MutableStateFlow<StoreUiState>(StoreUiState.Loading)
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

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
