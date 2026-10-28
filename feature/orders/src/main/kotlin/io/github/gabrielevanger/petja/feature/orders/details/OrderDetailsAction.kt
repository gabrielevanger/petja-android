package io.github.gabrielevanger.petja.feature.orders.details

/** Tudo o que os detalhes do pedido pedem ao ViewModel. */
sealed interface OrderDetailsAction {
    /** A tela ficou visível: carrega na primeira vez e liga o status ao vivo. */
    data object OnStart : OrderDetailsAction

    /** A tela saiu de vista: desliga o status ao vivo. */
    data object OnStop : OrderDetailsAction

    data object OnBackClick : OrderDetailsAction

    data object OnRetryClick : OrderDetailsAction

    data object OnCancelClick : OrderDetailsAction

    data object OnConfirmCancel : OrderDetailsAction

    data object OnDismissCancel : OrderDetailsAction

    data object OnReorderClick : OrderDetailsAction

    data class OnWhatsappClick(val number: String) : OrderDetailsAction
}
