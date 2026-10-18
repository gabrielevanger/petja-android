package io.github.gabrielevanger.xodopet.core.domain.cart

import io.github.gabrielevanger.xodopet.core.domain.model.Cart
import kotlinx.coroutines.flow.Flow

/** Carrinho salvo no aparelho, para não sumir ao fechar o app (RF-42). */
interface CartRepository {
    /** null quando o carrinho está vazio. */
    val cart: Flow<Cart?>

    /** Lê, transforma e grava de uma vez, sem perder alterações feitas ao mesmo tempo. */
    suspend fun update(transform: (Cart?) -> Cart?)
}
