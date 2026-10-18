package io.github.gabrielevanger.xodopet.core.domain.testing

import io.github.gabrielevanger.xodopet.core.domain.checkout.PaymentChoice
import io.github.gabrielevanger.xodopet.core.domain.model.Money
import io.github.gabrielevanger.xodopet.core.domain.model.OrderAddress
import io.github.gabrielevanger.xodopet.core.domain.model.OrderItem
import io.github.gabrielevanger.xodopet.core.domain.model.OrderStatus
import io.github.gabrielevanger.xodopet.core.domain.storemode.ManagedStore
import io.github.gabrielevanger.xodopet.core.domain.storemode.StoreOrder
import io.github.gabrielevanger.xodopet.core.domain.storemode.StoreOrderError
import io.github.gabrielevanger.xodopet.core.domain.storemode.StoreOrderException
import io.github.gabrielevanger.xodopet.core.domain.storemode.StoreOrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

val LAVRADO = ManagedStore("store-1", "Petshop Lavrado")

fun aStoreOrder(
    id: String = "pedido-1",
    number: Long = 1,
    status: OrderStatus = OrderStatus.Pending,
    payment: PaymentChoice = PaymentChoice.Cash(Money(10000)),
) = StoreOrder(
    id = id,
    number = number,
    status = status,
    statusReason = null,
    customerName = "Ana Souza",
    customerPhone = "95991234567",
    items = listOf(OrderItem("1kg", "Ração Golden", "1 kg", Money(3090), quantity = 2)),
    total = Money(6680),
    payment = payment,
    notes = "Tocar a campainha",
    address = OrderAddress("Rua Cerro Corá", "78", null, "Centro", "Perto da praça"),
    createdAt = ORDER_TIME,
)

/** Mudanças ficam em [transitions]; `failWith` faz a próxima chamada falhar com o motivo dado. */
class FakeStoreOrderRepository(
    var stores: List<ManagedStore> = listOf(LAVRADO),
    var orders: List<StoreOrder> = listOf(aStoreOrder()),
) : StoreOrderRepository {
    var failWith: StoreOrderError? = null
    val transitions = mutableListOf<Triple<String, OrderStatus, String?>>()
    val ordersCalls = mutableListOf<String>()
    private val changeEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Simula o Realtime avisando que algo mudou na loja. */
    fun emitChange() {
        changeEvents.tryEmit(Unit)
    }

    private fun failIfAsked() {
        failWith?.let {
            failWith = null
            throw StoreOrderException(it)
        }
    }

    override suspend fun managedStores(): List<ManagedStore> {
        failIfAsked()
        return stores
    }

    override suspend fun orders(storeId: String): List<StoreOrder> {
        failIfAsked()
        ordersCalls += storeId
        return orders
    }

    override suspend fun transition(orderId: String, to: OrderStatus, reason: String?) {
        failIfAsked()
        transitions += Triple(orderId, to, reason)
        orders = orders.map { if (it.id == orderId) it.copy(status = to, statusReason = reason) else it }
    }

    override fun changes(storeId: String): Flow<Unit> = changeEvents
}
