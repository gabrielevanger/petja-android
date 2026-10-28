package io.github.gabrielevanger.petja.feature.orders.list

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
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrdersScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun listShowsOrdersAndOpensOne() {
        var opened: String? = null
        composeRule.setContent {
            PetjaTheme {
                OrdersScreen(
                    OrdersUiState.Loaded(
                        persistentListOf(
                            anOrderSummary(),
                            anOrderSummary(id = "pedido-2", status = OrderStatus.Canceled).copy(itemCount = 1),
                        ),
                    ),
                    onAction = { if (it is OrdersAction.OnOrderClick) opened = it.orderId },
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
            PetjaTheme { OrdersScreen(OrdersUiState.NotSignedIn) { if (it == OrdersAction.OnSignInClick) signIn++ } }
        }

        composeRule.onNodeWithText("Entrar").performClick()
        assertEquals(1, signIn)
    }

    @Test
    fun emptyList() {
        composeRule.setContent { PetjaTheme { OrdersScreen(OrdersUiState.Loaded(persistentListOf())) {} } }

        composeRule.onNodeWithText("Você ainda não fez nenhum pedido.").assertExists()
    }
}
