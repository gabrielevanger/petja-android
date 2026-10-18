package io.github.gabrielevanger.xodopet.feature.orders

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme
import io.github.gabrielevanger.xodopet.core.domain.checkout.PaymentChoice
import io.github.gabrielevanger.xodopet.core.domain.model.Money
import io.github.gabrielevanger.xodopet.core.domain.model.OrderStatus
import io.github.gabrielevanger.xodopet.core.domain.model.StatusChange
import io.github.gabrielevanger.xodopet.core.domain.testing.ORDER_TIME
import io.github.gabrielevanger.xodopet.core.domain.testing.anOrderDetails
import io.github.gabrielevanger.xodopet.core.domain.testing.anOrderSummary
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrdersScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun listShowsOrdersAndOpensOne() {
        var opened: String? = null
        composeRule.setContent {
            XodoTheme {
                OrdersScreen(
                    OrdersUiState.Loaded(
                        persistentListOf(
                            anOrderSummary(),
                            anOrderSummary(id = "pedido-2", status = OrderStatus.Canceled).copy(itemCount = 1),
                        ),
                    ),
                    onBack = {},
                    onOpenOrder = { opened = it },
                    onRetry = {},
                    onSignIn = {},
                )
            }
        }

        composeRule.onNodeWithText("Aguardando a loja").assertExists()
        composeRule.onNodeWithText("Cancelado").assertExists()
        composeRule.onNodeWithText("2 itens", substring = true).assertExists()
        composeRule.onNodeWithText("1 item", substring = true).assertExists()
        composeRule.onNodeWithText("Aguardando a loja").performClick()
        assertEquals("pedido-1", opened)
    }

    @Test
    fun listStates() {
        var signIn = 0
        composeRule.setContent {
            XodoTheme { OrdersScreen(OrdersUiState.NotSignedIn, {}, {}, {}, onSignIn = { signIn++ }) }
        }

        composeRule.onNodeWithText("Entrar").performClick()
        assertEquals(1, signIn)
    }

    @Test
    fun emptyList() {
        composeRule.setContent { XodoTheme { OrdersScreen(OrdersUiState.Loaded(persistentListOf()), {}, {}, {}, {}) } }

        composeRule.onNodeWithText("Você ainda não fez nenhum pedido.").assertExists()
    }

    @Test
    fun detailsOfAPendingOrder() {
        var cancel = 0
        var reorder = 0
        composeRule.setContent {
            XodoTheme {
                OrderDetailsScreen(
                    OrderDetailsUiState.Loaded(anOrderDetails()),
                    OrderDetailsActions(onAskCancel = { cancel++ }, onReorder = { reorder++ }),
                )
            }
        }

        composeRule.onNodeWithText("Pedido #1").assertExists()
        composeRule.onNodeWithText("Atualiza sozinho", substring = true).assertExists()
        composeRule.onNodeWithText("Cancelar pedido").performClick()
        composeRule.onNodeWithText("Pedir de novo").performClick()
        composeRule.onNodeWithText("Falar com a loja").assertExists()
        composeRule.onNodeWithText("1× Ração Golden", substring = true).performScrollTo().assertExists()
        composeRule.onNodeWithText("Dinheiro · troco para R$ 100,00").performScrollTo().assertExists()
        composeRule.onNodeWithText("Rua Cerro Corá, 78", substring = true).performScrollTo().assertExists()
        composeRule.onNodeWithText("Observação: Tocar a campainha").performScrollTo().assertExists()
        assertEquals(1, cancel)
        assertEquals(1, reorder)
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
            XodoTheme {
                OrderDetailsScreen(
                    OrderDetailsUiState.Loaded(order, confirmingCancel = true, message = OrderMessage.CannotCancel),
                    OrderDetailsActions(onConfirmCancel = { confirmed++ }),
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
            XodoTheme {
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
            XodoTheme { OrderDetailsScreen(OrderDetailsUiState.Error, OrderDetailsActions(onRetry = { retries++ })) }
        }

        composeRule.onNodeWithText("Tentar de novo").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun cardAndCashWithoutChange() {
        composeRule.setContent {
            XodoTheme {
                Column {
                    OrderDetailsScreen(
                        OrderDetailsUiState.Loaded(anOrderDetails(payment = PaymentChoice.CardOnDelivery)),
                        OrderDetailsActions(),
                    )
                }
            }
        }

        composeRule.onNodeWithText("Cartão na maquininha").performScrollTo().assertExists()
    }
}
