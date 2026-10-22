package io.github.gabrielevanger.petja.feature.storefront

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gabrielevanger.petja.core.domain.DiagnosticLog
import io.github.gabrielevanger.petja.core.domain.cart.CartRepository
import io.github.gabrielevanger.petja.core.domain.delivery.DeliveryPlaceRepository
import io.github.gabrielevanger.petja.core.domain.model.DeliveryPlace
import io.github.gabrielevanger.petja.core.domain.storefront.LoadStorefront
import io.github.gabrielevanger.petja.core.ui.toCartBar
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class StorefrontViewModel @Inject constructor(
    private val loadStorefront: LoadStorefront,
    private val deliveryPlaces: DeliveryPlaceRepository,
    private val cartRepository: CartRepository,
    private val log: DiagnosticLog,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StorefrontUiState(
            places = BoaVistaSamplePlaces.toImmutableList(),
            selectedPlace = BoaVistaSamplePlaces.first(),
        ),
    )
    val uiState: StateFlow<StorefrontUiState> = _uiState.asStateFlow()

    private val _events = Channel<StorefrontEvent>(Channel.BUFFERED)
    val events: Flow<StorefrontEvent> = _events.receiveAsFlow()

    /** A vitrine carrega uma vez só por ViewModel; a rotação de tela não repete a busca. */
    private var started = false
    private var loading: Job? = null
    private var listeningCart: Job? = null

    fun onAction(action: StorefrontAction) {
        when (action) {
            StorefrontAction.OnStart -> onStart()
            StorefrontAction.OnStop -> listeningCart?.cancel()
            is StorefrontAction.OnPlaceClick -> selectPlace(action.place)
            StorefrontAction.OnRetryClick -> load(_uiState.value.selectedPlace)
            is StorefrontAction.OnStoreClick -> send(StorefrontEvent.OpenStore(action.storeId))
            StorefrontAction.OnCartClick -> send(StorefrontEvent.OpenCart)
            StorefrontAction.OnAccountClick -> send(StorefrontEvent.OpenAccount)
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
        // Volta ao lugar escolhido da última vez; sem escolha salva, começa pelo primeiro. Se o cliente
        // tocar num lugar antes de a leitura terminar, a escolha dele vale e a leitura atrasada é ignorada.
        viewModelScope.launch {
            val saved = deliveryPlaces.place.first()
            if (loading == null) {
                load(BoaVistaSamplePlaces.firstOrNull { it.name == saved?.name } ?: BoaVistaSamplePlaces.first())
            }
        }
    }

    private fun selectPlace(place: SamplePlace) {
        if (place == _uiState.value.selectedPlace && _uiState.value.content !is StorefrontContent.Error) return
        load(place)
    }

    /**
     * Lugar e "carregando" mudam juntos, para a tela nunca mostrar o lugar novo com as lojas do anterior.
     * Trocar de lugar no meio de uma busca cancela a anterior: a resposta velha nunca sobrescreve a nova.
     */
    private fun load(place: SamplePlace) {
        loading?.cancel()
        loading = flow {
            deliveryPlaces.set(DeliveryPlace(place.name, place.point))
            emit(loadStorefront(place.point).toContent())
        }
            .onStart { _uiState.update { it.copy(selectedPlace = place, content = StorefrontContent.Loading) } }
            .onEach { content -> _uiState.update { it.copy(content = content) } }
            .catch { error ->
                log.warn("Falha ao carregar a vitrine de ${place.name}", error)
                _uiState.update { it.copy(content = StorefrontContent.Error) }
            }
            .launchIn(viewModelScope)
    }

    private fun send(event: StorefrontEvent) {
        _events.trySend(event)
    }
}
