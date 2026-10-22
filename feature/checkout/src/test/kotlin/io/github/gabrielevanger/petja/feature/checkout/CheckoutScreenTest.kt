package io.github.gabrielevanger.petja.feature.checkout

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme
import io.github.gabrielevanger.petja.core.domain.checkout.CheckoutError
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CheckoutScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val ready = CheckoutUiState.Ready(
        storeName = "Petshop Lavrado",
        itemCount = 2,
        addresses = persistentListOf(AddressUi("casa", "Rua Teste", "10", null, "Centro", "Perto da praça")),
        selectedAddressId = "casa",
        subtotal = "R$ 61,80",
        delivery = DeliveryUi.Fee("R$ 5,00", isFree = false, storeIsOpen = true),
        total = "R$ 66,80",
        minOrder = "R$ 30,00",
    )

    private fun show(state: CheckoutUiState, actions: CheckoutActions = CheckoutActions()) =
        composeRule.setContent { PetjaTheme { CheckoutScreen(state, actions) } }

    @Test
    fun readyCheckoutShowsAddressPaymentAndTotal() {
        var submitted = 0
        var payment: PaymentOption? = null
        show(ready, CheckoutActions(onSubmit = { submitted++ }, onSelectPayment = { payment = it }))

        composeRule.onNodeWithText("Rua Teste, 10").assertExists()
        composeRule.onNodeWithText("Centro · Perto da praça").assertExists()
        composeRule.onNodeWithText("Dinheiro").performScrollTo().performClick()
        assertEquals(PaymentOption.Cash, payment)
        composeRule.onNodeWithText("R$ 66,80").assertExists()
        composeRule.onNodeWithText("Fazer pedido").assertIsEnabled().performClick()
        assertEquals(1, submitted)
    }

    @Test
    fun cashShowsTheChangeFieldAndItsError() {
        var change = ""
        show(
            ready.copy(payment = PaymentOption.Cash, changeForInvalid = true),
            CheckoutActions(onChangeForChange = {
                change =
                    it
            }),
        )

        composeRule.onNodeWithText("Escreva só o valor, como 100 ou 100,50.").performScrollTo().assertExists()
        composeRule.onNodeWithText("Troco para quanto? (opcional)").performTextInput("50")
        assertEquals("50", change)
    }

    @Test
    fun newAddressForm() {
        var saved = 0
        var typed: Pair<AddressField, String>? = null
        val form = NewAddressForm(neighborhood = "Centro", placeName = "Centro", missing = setOf(AddressField.Street))
        show(
            ready.copy(newAddress = form, selectedAddressId = null, delivery = DeliveryUi.ChooseAddress, total = null),
            CheckoutActions(onSaveAddress = { saved++ }, onAddressChange = { field, value -> typed = field to value }),
        )

        composeRule.onNodeWithText("Preencha este campo.").assertExists()
        composeRule.onNodeWithText("Número").performTextInput("12")
        assertEquals(AddressField.Number to "12", typed)
        composeRule.onNodeWithText("Usar este endereço").performScrollTo().performClick()
        assertEquals(1, saved)
        composeRule.onNodeWithText("Por enquanto, o ponto no mapa", substring = true).performScrollTo().assertExists()
        composeRule.onNode(hasText("Fazer pedido") and hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun deliveryStatesAndMinimum() {
        show(ready.copy(delivery = DeliveryUi.OutOfArea, total = null, missingForMinOrder = "R$ 5,00"))

        composeRule.onNodeWithText("A loja não entrega nesse endereço.").performScrollTo().assertExists()
        composeRule.onNodeWithText("Faltam R$ 5,00", substring = true).performScrollTo().assertExists()
    }

    @Test
    fun notSignedInOffersSignIn() {
        var signIn = 0
        show(ready.copy(error = CheckoutError.NotSignedIn), CheckoutActions(onSignIn = { signIn++ }))

        composeRule.onNodeWithText("Entre na sua conta para fazer o pedido.").assertExists()
        composeRule.onNodeWithText("Entrar").performClick()
        assertEquals(1, signIn)
    }

    @Test
    fun emptyAndLoading() {
        show(CheckoutUiState.EmptyCart)

        composeRule.onNodeWithText("Seu carrinho está vazio.").assertExists()
    }

    @Test
    fun everyErrorHasItsOwnMessage() {
        val messages = mutableListOf<String>()
        composeRule.setContent {
            PetjaTheme {
                Column {
                    CheckoutError.entries.forEach { error ->
                        val text = error.message()
                        messages += text
                        Text(text)
                    }
                }
            }
        }

        assertEquals(CheckoutError.entries.size, messages.size)
    }

    @Test
    fun orderPlaced() {
        var back = 0
        var track = 0
        composeRule.setContent {
            PetjaTheme {
                OrderPlacedScreen(42, "Petshop Lavrado", "R$ 66,80", onBackToStorefront = {
                    back++
                }, onTrackOrder = { track++ })
            }
        }

        composeRule.onNodeWithText("Pedido #42", substring = true).assertExists()
        composeRule.onNodeWithText("A Petshop Lavrado tem 10 minutos", substring = true).assertExists()
        composeRule.onNodeWithText("Total R$ 66,80").assertExists()
        composeRule.onNodeWithText("Voltar para a vitrine").performClick()
        composeRule.onNodeWithText("Acompanhar pedido").performClick()
        assertEquals(1, back)
        assertEquals(1, track)
    }
}
