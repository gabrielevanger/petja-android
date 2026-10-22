package io.github.gabrielevanger.petja.feature.storemode

import androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme
import io.github.gabrielevanger.petja.core.domain.checkout.PaymentChoice
import io.github.gabrielevanger.petja.core.domain.model.OrderStatus
import io.github.gabrielevanger.petja.core.domain.storemode.ManagedStore
import io.github.gabrielevanger.petja.core.domain.storemode.StoreAction
import io.github.gabrielevanger.petja.core.domain.testing.LAVRADO
import io.github.gabrielevanger.petja.core.domain.testing.aStoreOrder
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoreModeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(state: StoreModeUiState, actions: StoreModeActions = StoreModeActions()) =
        composeRule.setContent { PetjaTheme { StoreModeScreen(state, actions) } }

    @Test
    fun newOrderShowsWhatToPrepareAndTheNextStep() {
        var acted: Pair<String, StoreAction>? = null
        show(
            StoreModeUiState.Loaded(persistentListOf(LAVRADO), LAVRADO.id, persistentListOf(aStoreOrder())),
            StoreModeActions(onAct = { order, action -> acted = order.id to action }),
        )

        composeRule.onNodeWithText("Petshop Lavrado").assertExists()
        composeRule.onNodeWithText("Novos").assertExists()
        composeRule.onNodeWithText("#1 · Ana Souza").assertExists()
        composeRule.onNodeWithText("2× Ração Golden 1 kg").assertExists()
        composeRule.onNodeWithText("Rua Cerro Corá, 78", substring = true).performScrollTo().assertExists()
        composeRule.onNodeWithText("Dinheiro · levar troco para R$ 100,00").performScrollTo().assertExists()
        composeRule.onNodeWithText("Observação: Tocar a campainha").performScrollTo().assertExists()
        composeRule.onNodeWithText("Aceitar").performScrollTo().performClick()
        assertEquals("pedido-1" to StoreAction.Accept, acted)
        composeRule.onNodeWithText("Recusar").performScrollTo().performClick()
        assertEquals("pedido-1" to StoreAction.Reject, acted)
    }

    @Test
    fun ordersAreGroupedByStepAndLockedWhileWorking() {
        show(
            StoreModeUiState.Loaded(
                stores = persistentListOf(LAVRADO, ManagedStore("store-2", "Agropet Caçari")),
                storeId = LAVRADO.id,
                orders = persistentListOf(
                    aStoreOrder(
                        id = "a",
                        number = 3,
                        status = OrderStatus.OutForDelivery,
                        payment = PaymentChoice.PixOnDelivery,
                    ),
                    aStoreOrder(
                        id = "b",
                        number = 2,
                        status = OrderStatus.Canceled,
                        payment = PaymentChoice.CardOnDelivery,
                    ),
                ),
                working = "a",
            ),
        )

        composeRule.onNodeWithText("Agropet Caçari").assertExists()
        composeRule.onNodeWithText("Em andamento").assertExists()
        composeRule.onNode(hasText("Não foi possível entregar") and hasClickAction()).performScrollTo()
            .assertIsNotEnabled()
        composeRule.onNodeWithText("Pix na entrega").assertExists()
        // A lista é preguiçosa: o que está lá embaixo só existe depois de a lista rolar até ele.
        // Com duas lojas há também a fileira de chips, que rola na horizontal: a lista é a vertical.
        composeRule.onNode(hasScrollToNodeAction() and SemanticsMatcher.keyIsDefined(VerticalScrollAxisRange))
            .performScrollToNode(hasText("Cartão na maquininha"))
        composeRule.onNodeWithText("Encerrados nas últimas 24 horas").assertExists()
        composeRule.onNodeWithText("Cartão na maquininha").assertExists()
    }

    @Test
    fun reasonDialogSendsWhatWasTyped() {
        var reason: String? = null
        show(
            StoreModeUiState.Loaded(
                persistentListOf(LAVRADO),
                LAVRADO.id,
                persistentListOf(aStoreOrder()),
                askingReason = ReasonRequest("pedido-1", 1, StoreAction.Reject),
            ),
            StoreModeActions(onConfirmReason = { reason = it }),
        )

        composeRule.onNodeWithText("Recusar o pedido #1?").assertExists()
        composeRule.onNode(hasText("Confirmar") and hasClickAction()).assertIsNotEnabled()
        composeRule.onNodeWithText("Motivo").performTextInput("Sem estoque")
        composeRule.onNodeWithText("Confirmar").performClick()
        assertEquals("Sem estoque", reason)
    }

    @Test
    fun otherStates() {
        var retried = 0
        show(StoreModeUiState.Error, StoreModeActions(onRetry = { retried++ }))
        composeRule.onNodeWithText("Tentar de novo").performClick()
        assertEquals(1, retried)
    }

    @Test
    fun emptyStoreWaitsForTheFirstOrder() {
        show(StoreModeUiState.Loaded(persistentListOf(LAVRADO), LAVRADO.id, persistentListOf()))
        composeRule.onNodeWithText("Nenhum pedido agora", substring = true).assertExists()
    }

    @Test
    fun noStoreAndSignedOut() {
        var signIn = 0
        show(StoreModeUiState.NotSignedIn, StoreModeActions(onSignIn = { signIn++ }))
        composeRule.onNodeWithText("Entrar").performClick()
        assertEquals(1, signIn)
    }
}
