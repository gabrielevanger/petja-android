package io.github.gabrielevanger.xodopet.feature.orders

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
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
