package io.github.gabrielevanger.xodopet.core.domain.checkout

import io.github.gabrielevanger.xodopet.core.domain.model.Money

/** Pagamento na entrega (RF-51). Pagamento online fica para a V2 (RF-54). */
sealed interface PaymentChoice {
    /** @property changeFor troco para quanto; null quando o cliente tem o valor certo. */
    data class Cash(val changeFor: Money?) : PaymentChoice

    data object PixOnDelivery : PaymentChoice

    data object CardOnDelivery : PaymentChoice
}

/** Pedido criado no servidor. O número curto é o que o cliente fala para a loja. */
data class PlacedOrder(val id: String, val number: Long, val total: Money)

/** Motivos de recusa do pedido, vindos das regras do place_order no banco (RN-01 a RN-10). */
enum class CheckoutError {
    NotSignedIn,
    StoreClosed,
    OutOfDeliveryArea,
    ItemUnavailable,
    InsufficientStock,
    BelowMinOrder,
    PriceChanged,
    ChangeTooLow,
    EmptyCart,
    Network,
    Unknown,
}

class CheckoutException(val error: CheckoutError, cause: Throwable? = null) : Exception(error.name, cause)
