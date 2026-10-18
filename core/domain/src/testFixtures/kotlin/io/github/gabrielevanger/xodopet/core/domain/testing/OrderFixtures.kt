package io.github.gabrielevanger.xodopet.core.domain.testing

import io.github.gabrielevanger.xodopet.core.domain.checkout.PaymentChoice
import io.github.gabrielevanger.xodopet.core.domain.model.Money
import io.github.gabrielevanger.xodopet.core.domain.model.OrderAddress
import io.github.gabrielevanger.xodopet.core.domain.model.OrderDetails
import io.github.gabrielevanger.xodopet.core.domain.model.OrderItem
import io.github.gabrielevanger.xodopet.core.domain.model.OrderStatus
import io.github.gabrielevanger.xodopet.core.domain.model.OrderSummary
import io.github.gabrielevanger.xodopet.core.domain.model.StatusChange
import io.github.gabrielevanger.xodopet.core.domain.orders.OrderError
import io.github.gabrielevanger.xodopet.core.domain.orders.OrderException
import io.github.gabrielevanger.xodopet.core.domain.orders.OrderRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

val ORDER_TIME: Instant = Instant.parse("2026-10-18T14:15:00Z")

fun anOrderSummary(id: String = "pedido-1", status: OrderStatus = OrderStatus.Pending) = OrderSummary(
    id = id,
    number = 1,
    storeName = "Petshop Lavrado",
    status = status,
    total = Money(7680),
    itemCount = 2,
    createdAt = ORDER_TIME,
)

fun anOrderDetails(
    id: String = "pedido-1",
    status: OrderStatus = OrderStatus.Pending,
    items: List<OrderItem> = listOf(
        OrderItem("1kg", "Ração Golden", "1 kg", Money(3090), quantity = 1),
        OrderItem("granplus", "Ração GranPlus", "1 kg", Money(4090), quantity = 1),
    ),
    payment: PaymentChoice = PaymentChoice.Cash(Money(10000)),
) = OrderDetails(
    id = id,
    number = 1,
    storeId = "store-1",
    storeName = "Petshop Lavrado",
    storeWhatsapp = "5595900000001",
    status = status,
    statusReason = null,
    items = items,
    subtotal = Money(7180),
    deliveryFee = Money(500),
    total = Money(7680),
    payment = payment,
    notes = "Tocar a campainha",
    address = OrderAddress("Rua Cerro Corá", "78", null, "Centro", "Perto da praça"),
    courierName = null,
    history = listOf(StatusChange(OrderStatus.Pending, ORDER_TIME, null)),
    createdAt = ORDER_TIME,
)

/** Pedidos em memória. `emitChange` simula o Realtime avisando que o pedido mudou. */
class FakeOrderRepository(
    var orders: List<OrderSummary> = listOf(anOrderSummary()),
    var details: Map<String, OrderDetails> = mapOf("pedido-1" to anOrderDetails()),
) : OrderRepository {
    var failWith: OrderError? = null
    val canceled = mutableListOf<String>()
    private val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun emitChange() = changes.tryEmit(Unit)

    private fun failIfAsked() {
        failWith?.let {
            failWith = null
            throw OrderException(it)
        }
    }

    override suspend fun myOrders(): List<OrderSummary> {
        failIfAsked()
        return orders
    }

    override suspend fun order(orderId: String): OrderDetails? {
        failIfAsked()
        return details[orderId]
    }

    override suspend fun cancel(orderId: String) {
        failIfAsked()
        canceled += orderId
        details = details + (orderId to details.getValue(orderId).copy(status = OrderStatus.Canceled))
    }

    override fun changes(orderId: String): Flow<Unit> = changes
}
