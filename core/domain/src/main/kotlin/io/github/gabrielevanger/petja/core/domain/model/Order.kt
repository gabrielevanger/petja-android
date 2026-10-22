package io.github.gabrielevanger.petja.core.domain.model

import io.github.gabrielevanger.petja.core.domain.checkout.PaymentChoice
import java.time.Instant

/** Máquina de estados do pedido (REQUISITOS.md, seção 4.1), na mesma ordem do banco. */
enum class OrderStatus {
    Pending,
    Accepted,
    Preparing,
    OutForDelivery,
    Delivered,
    NotDelivered,
    Rejected,
    Canceled,
    ;

    val isFinal: Boolean get() = this in setOf(Delivered, NotDelivered, Rejected, Canceled)

    /** O cliente só cancela enquanto a loja não aceitou (RN-05). */
    val customerCanCancel: Boolean get() = this == Pending
}

/** Pedido na lista "Meus pedidos" (RF-61). */
data class OrderSummary(
    val id: String,
    val number: Long,
    val storeName: String,
    val status: OrderStatus,
    val total: Money,
    val itemCount: Int,
    val createdAt: Instant,
)

data class OrderItem(
    val variantId: String?,
    val productName: String,
    val variantLabel: String,
    val unitPrice: Money,
    val quantity: Int,
) {
    val total: Money get() = Money(unitPrice.cents * quantity)
}

data class StatusChange(val status: OrderStatus, val at: Instant, val reason: String?)

/** Endereço copiado no pedido: mudar o endereço salvo depois não altera um pedido feito (RN-03). */
data class OrderAddress(
    val street: String,
    val number: String,
    val complement: String?,
    val neighborhood: String,
    val reference: String?,
)

data class OrderDetails(
    val id: String,
    val number: Long,
    val storeId: String,
    val storeName: String,
    val storeWhatsapp: String?,
    val status: OrderStatus,
    val statusReason: String?,
    val items: List<OrderItem>,
    val subtotal: Money,
    val deliveryFee: Money,
    val total: Money,
    val payment: PaymentChoice,
    val notes: String?,
    val address: OrderAddress,
    val courierName: String?,
    /** Mais antigas primeiro. */
    val history: List<StatusChange>,
    val createdAt: Instant,
)
