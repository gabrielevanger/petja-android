package io.github.gabrielevanger.xodopet.core.data.remote

import io.github.gabrielevanger.xodopet.core.data.remote.dto.OrderDetailsDto
import io.github.gabrielevanger.xodopet.core.data.remote.dto.OrderSummaryDto
import io.github.gabrielevanger.xodopet.core.domain.checkout.PaymentChoice
import io.github.gabrielevanger.xodopet.core.domain.model.Money
import io.github.gabrielevanger.xodopet.core.domain.model.OrderAddress
import io.github.gabrielevanger.xodopet.core.domain.model.OrderDetails
import io.github.gabrielevanger.xodopet.core.domain.model.OrderItem
import io.github.gabrielevanger.xodopet.core.domain.model.OrderStatus
import io.github.gabrielevanger.xodopet.core.domain.model.OrderSummary
import io.github.gabrielevanger.xodopet.core.domain.model.StatusChange
import java.time.Instant
import java.time.OffsetDateTime

/** Nome usado quando a loja do pedido não está mais visível (desativada depois do pedido). */
private const val UNKNOWN_STORE = ""

/** O Postgres manda as datas com fuso (+00:00); OffsetDateTime lê isso em todas as versões do Android do app. */
internal fun parseTimestamp(value: String): Instant = OffsetDateTime.parse(value).toInstant()

/** pending -> Pending, out_for_delivery -> OutForDelivery. Status novo no banco vira Pending até o app atualizar. */
internal fun parseStatus(value: String): OrderStatus =
    OrderStatus.entries.firstOrNull { it.name.equals(value.replace("_", ""), ignoreCase = true) } ?: OrderStatus.Pending

internal fun OrderSummaryDto.toDomain() = OrderSummary(
    id = id,
    number = number,
    storeName = store?.name ?: UNKNOWN_STORE,
    status = parseStatus(status),
    total = Money(totalCents),
    itemCount = items.sumOf { it.quantity },
    createdAt = parseTimestamp(createdAt),
)

/** Pending -> pending, OutForDelivery -> out_for_delivery: o nome do enum no banco. */
internal fun OrderStatus.toDbValue(): String = name.replace(Regex("(?<=[a-z])(?=[A-Z])"), "_").lowercase()

internal fun parsePayment(method: String, changeForCents: Long?): PaymentChoice = when (method) {
    "pix_on_delivery" -> PaymentChoice.PixOnDelivery
    "card_on_delivery" -> PaymentChoice.CardOnDelivery
    else -> PaymentChoice.Cash(changeForCents?.let(::Money))
}

internal fun OrderDetailsDto.toDomain() = OrderDetails(
    id = id,
    number = number,
    storeId = storeId,
    storeName = store?.name ?: UNKNOWN_STORE,
    storeWhatsapp = store?.whatsapp?.takeIf { it.isNotBlank() },
    status = parseStatus(status),
    statusReason = statusReason,
    items = items.map {
        OrderItem(it.variantId, it.productName, it.variantLabel, Money(it.unitPriceCents), it.quantity)
    },
    subtotal = Money(subtotalCents),
    deliveryFee = Money(deliveryFeeCents),
    total = Money(totalCents),
    payment = parsePayment(paymentMethod, changeForCents),
    notes = notes,
    address = OrderAddress(street, numberAddress, complement, neighborhood, reference),
    courierName = courierName,
    history = history.map {
        StatusChange(parseStatus(it.toStatus), parseTimestamp(it.createdAt), it.reason)
    }.sortedBy { it.at },
    createdAt = parseTimestamp(createdAt),
)
