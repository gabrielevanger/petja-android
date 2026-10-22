package io.github.gabrielevanger.petja.core.domain.orders

import io.github.gabrielevanger.petja.core.domain.model.OrderDetails
import io.github.gabrielevanger.petja.core.domain.model.OrderSummary
import kotlinx.coroutines.flow.Flow

enum class OrderError { NotSignedIn, CannotCancel, Network, Unknown }

class OrderException(val error: OrderError, cause: Throwable? = null) : Exception(error.name, cause)

/** Pedidos do cliente logado. Toda falha chega como [OrderException]. */
interface OrderRepository {
    /** Mais recentes primeiro (RF-61). */
    suspend fun myOrders(): List<OrderSummary>

    /** null quando o pedido não existe ou não é do cliente (a RLS esconde). */
    suspend fun order(orderId: String): OrderDetails?

    /** Cancela um pedido pendente (RN-05). */
    suspend fun cancel(orderId: String)

    /** Emite sempre que o pedido muda no servidor, enquanto alguém escuta (RF-60). */
    fun changes(orderId: String): Flow<Unit>
}
