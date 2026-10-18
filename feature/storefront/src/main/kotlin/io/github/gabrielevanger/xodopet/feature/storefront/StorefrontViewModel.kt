package io.github.gabrielevanger.xodopet.feature.storefront

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gabrielevanger.xodopet.core.domain.DiagnosticLog
import io.github.gabrielevanger.xodopet.core.domain.storefront.LoadStorefront
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class StorefrontViewModel @Inject constructor(
    private val loadStorefront: LoadStorefront,
    private val log: DiagnosticLog,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StorefrontUiState(
            places = BoaVistaSamplePlaces.toImmutableList(),
            selectedPlace = BoaVistaSamplePlaces.first(),
        ),
    )
    val uiState: StateFlow<StorefrontUiState> = _uiState.asStateFlow()

    private var loading: Job? = null

    init {
        load(_uiState.value.selectedPlace)
    }

    fun selectPlace(place: SamplePlace) {
        if (place == _uiState.value.selectedPlace && _uiState.value.content !is StorefrontContent.Error) return
        load(place)
    }

    fun retry() = load(_uiState.value.selectedPlace)

    /**
     * Lugar e "carregando" mudam juntos, para a tela nunca mostrar o lugar novo com as lojas do anterior.
     * Trocar de lugar no meio de uma busca cancela a anterior: a resposta velha nunca sobrescreve a nova.
     */
    private fun load(place: SamplePlace) {
        loading?.cancel()
        _uiState.update { it.copy(selectedPlace = place, content = StorefrontContent.Loading) }
        loading = viewModelScope.launch {
            val content = try {
                loadStorefront(place.point).toContent()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.warn("Falha ao carregar a vitrine de ${place.name}", e)
                StorefrontContent.Error
            }
            _uiState.update { it.copy(content = content) }
        }
    }
}
