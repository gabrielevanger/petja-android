package io.github.gabrielevanger.petja.feature.storemode

import androidx.compose.runtime.Immutable
import io.github.gabrielevanger.petja.core.domain.storemode.ManagedStore
import io.github.gabrielevanger.petja.core.domain.storemode.StoreAction
import io.github.gabrielevanger.petja.core.domain.storemode.StoreOrder
import kotlinx.collections.immutable.ImmutableList

@Immutable
sealed interface StoreModeUiState {
    data object Loading : StoreModeUiState

    data object NotSignedIn : StoreModeUiState

    /** A conta não é dona nem atendente de nenhuma loja. */
    data object NoStore : StoreModeUiState

    data object Error : StoreModeUiState

    /**
     * @property working pedido com uma mudança de etapa a caminho do servidor; os botões dele ficam travados.
     * @property askingReason recusa ou não entrega esperando o motivo.
     */
    data class Loaded(
        val stores: ImmutableList<ManagedStore>,
        val storeId: String,
        val orders: ImmutableList<StoreOrder>,
        val working: String? = null,
        val askingReason: ReasonRequest? = null,
        val message: StoreMessage? = null,
    ) : StoreModeUiState {
        val storeName: String get() = stores.first { it.id == storeId }.name
    }
}

@Immutable
data class ReasonRequest(val orderId: String, val number: Long, val action: StoreAction)

enum class StoreMessage { ActionFailed, OutOfDate, NotAllowed }
