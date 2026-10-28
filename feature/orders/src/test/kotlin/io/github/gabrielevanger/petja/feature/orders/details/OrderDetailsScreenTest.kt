package io.github.gabrielevanger.petja.feature.orders.details

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme
import io.github.gabrielevanger.petja.core.domain.checkout.PaymentChoice
import io.github.gabrielevanger.petja.core.domain.model.Money
import io.github.gabrielevanger.petja.core.domain.model.OrderStatus
import io.github.gabrielevanger.petja.core.domain.model.StatusChange
import io.github.gabrielevanger.petja.core.domain.testing.ORDER_TIME
import io.github.gabrielevanger.petja.core.domain.testing.anOrderDetails
import io.github.gabrielevanger.petja.core.domain.testing.anOrderSummary
import io.github.gabrielevanger.petja.feature.orders.label
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class OrderDetailsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun detailsOfAPendingOrder() {
        val actions = mutableListOf<OrderDetailsAction>()
        composeRule.setContent {
            PetjaTheme {
                OrderDetailsScreen(
                    OrderDetailsUiState.Loaded(anOrderDetails()),
                    onAction = { actions += it },
                )
            }
        }

        composeRule.onNodeWithText("Pedido #1").assertExists()
        composeRule.onNodeWithText("Atualiza sozinho", substring = true).assertExists()
        // As ações ficam abaixo do cartão de status: rola até cada uma antes do toque.
        composeRule.onNodeWithText("Pedir de novo").performScrollTo().performClick()
        composeRule.onNodeWithText("Falar com a loja").performScrollTo().assertExists()
        composeRule.onNodeWithText("Cancelar pedido").performScrollTo().performClick()
        composeRule.onNodeWithText("1× Ração Golden", substring = true).performScrollTo().assertExists()
        composeRule.onNodeWithText("Dinheiro · troco para R$ 100,00").performScrollTo().assertExists()
        composeRule.onNodeWithText("Rua Cerro Corá, 78", substring = true).performScrollTo().assertExists()
        composeRule.onNodeWithText("Observação: Tocar a campainha").performScrollTo().assertExists()
        assertEquals(
            listOf(OrderDetailsAction.OnReorderClick, OrderDetailsAction.OnCancelClick),
            actions.filter { it == OrderDetailsAction.OnReorderClick || it == OrderDetailsAction.OnCancelClick },
        )
    }

    @Test
    fun finishedOrderWithReasonAndCancelDialog() {
        var confirmed = 0
        val order = anOrderDetails(status = OrderStatus.Canceled, payment = PaymentChoice.PixOnDelivery).copy(
            statusReason = "A loja não respondeu a tempo",
            courierName = "Carlos",
            deliveryFee = Money.ZERO,
            history = listOf(
                StatusChange(OrderStatus.Pending, ORDER_TIME, null),
                StatusChange(OrderStatus.Canceled, ORDER_TIME, "A loja não respondeu a tempo"),
            ),
        )
        composeRule.setContent {
            PetjaTheme {
                OrderDetailsScreen(
                    OrderDetailsUiState.Loaded(order, confirmingCancel = true),
                    onAction = { if (it == OrderDetailsAction.OnConfirmCancel) confirmed++ },
                    message = OrderMessage.CannotCancel,
                )
            }
        }

        composeRule.onNodeWithText("Cancelar o pedido?").assertExists()
        composeRule.onNodeWithText("Cancelar pedido").performClick()
        assertEquals(1, confirmed)
        composeRule.onNodeWithText("Entregador: Carlos").assertExists()
        composeRule.onNodeWithText("Pix na entrega").performScrollTo().assertExists()
        composeRule.onNodeWithText("Grátis").performScrollTo().assertExists()
    }

    @Test
    fun everyStatusAndMessageHasText() {
        val texts = mutableListOf<String>()
        composeRule.setContent {
            PetjaTheme {
                Column {
                    OrderStatus.entries.forEach { status -> status.label().also { texts += it }.let { Text(it) } }
                }
            }
        }

        assertEquals(OrderStatus.entries.size, texts.distinct().size)
    }

    @Test
    fun detailStates() {
        var retries = 0
        composeRule.setContent {
            PetjaTheme {
                OrderDetailsScreen(OrderDetailsUiState.Error, { if (it == OrderDetailsAction.OnRetryClick) retries++ })
            }
        }

        composeRule.onNodeWithText("Tentar de novo").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun cardAndCashWithoutChange() {
        composeRule.setContent {
            PetjaTheme {
                Column {
                    OrderDetailsScreen(
                        OrderDetailsUiState.Loaded(anOrderDetails(payment = PaymentChoice.CardOnDelivery)),
                        onAction = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Cartão na maquininha").performScrollTo().assertExists()
    }
}
