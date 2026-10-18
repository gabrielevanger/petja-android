package io.github.gabrielevanger.xodopet.feature.orders

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.gabrielevanger.xodopet.core.designsystem.R as DesignR
import io.github.gabrielevanger.xodopet.core.designsystem.component.BadgeTone
import io.github.gabrielevanger.xodopet.core.domain.model.OrderStatus

@Composable
internal fun OrderStatus.label(): String = stringResource(
    when (this) {
        OrderStatus.Pending -> R.string.status_pending
        OrderStatus.Accepted -> R.string.status_accepted
        OrderStatus.Preparing -> R.string.status_preparing
        OrderStatus.OutForDelivery -> R.string.status_out_for_delivery
        OrderStatus.Delivered -> R.string.status_delivered
        OrderStatus.NotDelivered -> R.string.status_not_delivered
        OrderStatus.Rejected -> R.string.status_rejected
        OrderStatus.Canceled -> R.string.status_canceled
    },
)

/** Em andamento e entregue em destaque; os que não deram certo, neutros. */
internal val OrderStatus.tone: BadgeTone
    get() = if (this in
        setOf(OrderStatus.NotDelivered, OrderStatus.Rejected, OrderStatus.Canceled)
    ) {
        BadgeTone.Neutral
    } else {
        BadgeTone.Positive
    }

/** Frase grande do cartão de status nos detalhes do pedido. */
@Composable
internal fun OrderStatus.headline(): String = stringResource(
    when (this) {
        OrderStatus.Pending -> R.string.headline_pending
        OrderStatus.Accepted -> R.string.headline_accepted
        OrderStatus.Preparing -> R.string.headline_preparing
        OrderStatus.OutForDelivery -> R.string.headline_out_for_delivery
        OrderStatus.Delivered -> R.string.headline_delivered
        OrderStatus.NotDelivered -> R.string.headline_not_delivered
        OrderStatus.Rejected -> R.string.headline_rejected
        OrderStatus.Canceled -> R.string.headline_canceled
    },
)

/** Ícone da situação: relógio enquanto espera, bicicleta na rua, caixa no fim. */
@get:DrawableRes
internal val OrderStatus.icon: Int
    get() = when (this) {
        OrderStatus.Pending, OrderStatus.Accepted, OrderStatus.Preparing -> DesignR.drawable.ic_clock
        OrderStatus.OutForDelivery -> DesignR.drawable.ic_bike
        OrderStatus.Delivered -> DesignR.drawable.ic_package_check
        OrderStatus.NotDelivered, OrderStatus.Rejected, OrderStatus.Canceled -> DesignR.drawable.ic_package_x
    }
