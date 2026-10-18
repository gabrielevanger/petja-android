package io.github.gabrielevanger.xodopet.feature.cart

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
sealed interface CartUiState {
    data object Loading : CartUiState

    data object Empty : CartUiState

    /**
     * @property total subtotal mais entrega; null enquanto a taxa não é conhecida.
     * @property missingForMinOrder quanto falta para o pedido mínimo; null quando já atingiu (RF-44).
     */
    data class Loaded(
        val storeName: String,
        val placeName: String?,
        val lines: ImmutableList<CartLineUi>,
        val subtotal: String,
        val delivery: DeliveryUi,
        val total: String?,
        val minOrder: String,
        val missingForMinOrder: String?,
    ) : CartUiState
}

@Immutable
data class CartLineUi(
    val variantId: String,
    val productName: String,
    val variantLabel: String,
    val unitPrice: String,
    val total: String,
    val quantity: Int,
    val canIncrease: Boolean,
)

/** Situação da taxa de entrega para o lugar escolhido na vitrine (RF-43). */
@Immutable
sealed interface DeliveryUi {
    data object Calculating : DeliveryUi

    data object NoPlace : DeliveryUi

    data object OutOfArea : DeliveryUi

    data object Error : DeliveryUi

    data class Fee(val value: String, val isFree: Boolean, val storeIsOpen: Boolean) : DeliveryUi
}
