package io.github.gabrielevanger.petja.core.data.local

import io.github.gabrielevanger.petja.core.domain.model.Cart
import io.github.gabrielevanger.petja.core.domain.model.CartLine
import io.github.gabrielevanger.petja.core.domain.model.Money
import kotlinx.serialization.Serializable

/** Formato do carrinho no arquivo. Separado do domínio para o modelo poder mudar sem perder carrinhos salvos. */
@Serializable
internal data class StoredCartFile(val cart: StoredCart? = null)

@Serializable
internal data class StoredCart(
    val storeId: String,
    val storeName: String,
    val minOrderCents: Long,
    val lines: List<StoredCartLine>,
)

@Serializable
internal data class StoredCartLine(
    val variantId: String,
    val productId: String,
    val productName: String,
    val variantLabel: String,
    val unitPriceCents: Long,
    val quantity: Int,
    val maxQuantity: Int,
)

internal fun StoredCart.toDomain() = Cart(
    storeId = storeId,
    storeName = storeName,
    minOrder = Money(minOrderCents),
    lines = lines.map {
        CartLine(
            it.variantId,
            it.productId,
            it.productName,
            it.variantLabel,
            Money(it.unitPriceCents),
            it.quantity,
            it.maxQuantity,
        )
    },
)

internal fun Cart.toStored() = StoredCart(
    storeId = storeId,
    storeName = storeName,
    minOrderCents = minOrder.cents,
    lines = lines.map {
        StoredCartLine(
            it.variantId,
            it.productId,
            it.productName,
            it.variantLabel,
            it.unitPrice.cents,
            it.quantity,
            it.maxQuantity,
        )
    },
)
