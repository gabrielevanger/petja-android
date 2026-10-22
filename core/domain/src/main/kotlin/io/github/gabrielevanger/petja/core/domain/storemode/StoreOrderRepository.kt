package io.github.gabrielevanger.petja.core.domain.storemode

import io.github.gabrielevanger.petja.core.domain.checkout.PaymentChoice
import io.github.gabrielevanger.petja.core.domain.model.Money
import io.github.gabrielevanger.petja.core.domain.model.OrderAddress
import io.github.gabrielevanger.petja.core.domain.model.OrderItem
import io.github.gabrielevanger.petja.core.domain.model.OrderStatus
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** Loja que a conta pode operar: é dona ou atendente (ou admin). */
data class ManagedStore(val id: String, val name: String)

/** Pedido do ponto de vista da loja: quem pediu, o que separar, para onde levar e quanto cobrar. */
data class StoreOrder(
    val id: String,
    val number: Long,
    val status: OrderStatus,
    val statusReason: String?,
    val customerName: String,
    val customerPhone: String?,
    val items: List<OrderItem>,
    val total: Money,
    val payment: PaymentChoice,
    val notes: String?,
    val address: OrderAddress,
    val createdAt: Instant,
)

/** O que a loja pode fazer com o pedido em cada etapa (RF-82, RF-83). */
enum class StoreAction(val to: OrderStatus, val needsReason: Boolean) {
    Accept(OrderStatus.Accepted, needsReason = false),
    Reject(OrderStatus.Rejected, needsReason = true),
    StartPreparing(OrderStatus.Preparing, needsReason = false),
    SendOut(OrderStatus.OutForDelivery, needsReason = false),
    Deliver(OrderStatus.Delivered, needsReason = false),
    NotDeliver(OrderStatus.NotDelivered, needsReason = true),
}

/** A primeira ação é o próximo passo normal; as outras são saídas de exceção (recusar, não entregue). */
val StoreOrder.actions: List<StoreAction>
    get() = when (status) {
        OrderStatus.Pending -> listOf(StoreAction.Accept, StoreAction.Reject)
        OrderStatus.Accepted -> listOf(StoreAction.StartPreparing)
        OrderStatus.Preparing -> listOf(StoreAction.SendOut)
        OrderStatus.OutForDelivery -> listOf(StoreAction.Deliver, StoreAction.NotDeliver)
        OrderStatus.Delivered, OrderStatus.NotDelivered, OrderStatus.Rejected, OrderStatus.Canceled -> emptyList()
    }

/**
 * Pedidos aguardando que não estavam na lista anterior: são os que tocam o alerta (RF-81).
 * Na primeira carga ([before] nulo) nada toca, para não apitar por pedidos que já estavam lá.
 */
fun newOrders(before: List<StoreOrder>?, after: List<StoreOrder>): List<StoreOrder> {
    if (before == null) return emptyList()
    val known = before.mapTo(HashSet()) { it.id }
    return after.filter { it.status == OrderStatus.Pending && it.id !in known }
}

enum class StoreOrderError { NotSignedIn, NotAllowed, InvalidTransition, ReasonRequired, Network, Unknown }

class StoreOrderException(val error: StoreOrderError, cause: Throwable? = null) : Exception(error.name, cause)

/** Pedidos das lojas que a conta opera. Toda falha chega como [StoreOrderException]. */
interface StoreOrderRepository {
    /** Ordenadas pelo nome; vazia quando a conta não é da equipe de nenhuma loja. */
    suspend fun managedStores(): List<ManagedStore>

    /** Em andamento e os encerrados nas últimas 24 horas, mais recentes primeiro. */
    suspend fun orders(storeId: String): List<StoreOrder>

    /** Muda a etapa pela função do servidor, que confere a máquina de estados e grava o histórico. */
    suspend fun transition(orderId: String, to: OrderStatus, reason: String?)

    /** Emite quando um pedido da loja chega ou muda, enquanto alguém escuta (RF-81). */
    fun changes(storeId: String): Flow<Unit>
}
