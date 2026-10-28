package io.github.gabrielevanger.petja.feature.checkout

import androidx.compose.runtime.Immutable
import io.github.gabrielevanger.petja.core.domain.checkout.CheckoutError
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
sealed interface CheckoutUiState {
    data object Loading : CheckoutUiState

    data object EmptyCart : CheckoutUiState

    data class Ready(
        val storeName: String,
        val itemCount: Int,
        val addresses: ImmutableList<AddressUi> = persistentListOf(),
        val selectedAddressId: String? = null,
        /** O formulário de endereço novo aparece quando não há endereço salvo ou o cliente pede outro. */
        val newAddress: NewAddressForm? = null,
        val payment: PaymentOption = PaymentOption.Pix,
        val changeFor: String = "",
        val changeForInvalid: Boolean = false,
        val notes: String = "",
        val subtotal: String,
        val delivery: DeliveryUi = DeliveryUi.ChooseAddress,
        /** null enquanto a taxa não é conhecida. */
        val total: String? = null,
        val missingForMinOrder: String? = null,
        val minOrder: String,
        val submitting: Boolean = false,
        val error: CheckoutError? = null,
    ) : CheckoutUiState {
        val canPlaceOrder: Boolean
            get() = !submitting && selectedAddressId != null && newAddress == null && total != null &&
                missingForMinOrder == null && (delivery as? DeliveryUi.Fee)?.storeIsOpen == true
    }
}

@Immutable
data class AddressUi(
    val id: String,
    val street: String,
    val number: String,
    val complement: String?,
    val neighborhood: String,
    val reference: String?,
)

enum class PaymentOption { Cash, Pix, Card }

/** Campos do endereço novo. O bairro vem preenchido com o lugar escolhido na vitrine. */
@Immutable
data class NewAddressForm(
    val street: String = "",
    val number: String = "",
    val complement: String = "",
    val neighborhood: String = "",
    val reference: String = "",
    val placeName: String,
    val missing: Set<AddressField> = emptySet(),
    val saving: Boolean = false,
)

enum class AddressField { Street, Number, Neighborhood, Complement, Reference }

@Immutable
sealed interface DeliveryUi {
    data object ChooseAddress : DeliveryUi

    data object Calculating : DeliveryUi

    data object OutOfArea : DeliveryUi

    data object Error : DeliveryUi

    data class Fee(val value: String, val isFree: Boolean, val storeIsOpen: Boolean) : DeliveryUi
}

@Immutable
data class PlacedOrderUi(val id: String, val number: Long, val storeName: String, val total: String)
