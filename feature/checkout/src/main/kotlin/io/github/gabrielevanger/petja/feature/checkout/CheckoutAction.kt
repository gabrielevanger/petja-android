package io.github.gabrielevanger.petja.feature.checkout

/** Tudo o que o checkout pede ao ViewModel. */
sealed interface CheckoutAction {
    /** A tela ficou visível: na primeira vez carrega carrinho, endereços e taxa. */
    data object OnStart : CheckoutAction

    data object OnBackClick : CheckoutAction

    data object OnSignInClick : CheckoutAction

    data class OnAddressClick(val addressId: String) : CheckoutAction

    data object OnNewAddressClick : CheckoutAction

    data object OnCancelNewAddressClick : CheckoutAction

    data class OnAddressFieldChange(val field: AddressField, val value: String) : CheckoutAction

    data object OnSaveAddressClick : CheckoutAction

    data class OnPaymentClick(val option: PaymentOption) : CheckoutAction

    data class OnChangeForChange(val text: String) : CheckoutAction

    data class OnNotesChange(val text: String) : CheckoutAction

    data object OnPlaceOrderClick : CheckoutAction
}
