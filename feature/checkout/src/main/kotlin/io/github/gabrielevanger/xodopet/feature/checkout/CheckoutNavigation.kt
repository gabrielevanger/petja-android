package io.github.gabrielevanger.xodopet.feature.checkout

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
data object CheckoutDestination

/** Tela de confirmação; leva tudo o que mostra, porque o carrinho já foi esvaziado. */
@Serializable
data class OrderPlacedDestination(val orderId: String, val number: Long, val storeName: String, val total: String)

fun NavController.navigateToCheckout() = navigate(CheckoutDestination) { launchSingleTop = true }

/**
 * @param onSignIn o checkout precisa de login; o app abre a tela de entrar e volta para cá.
 * @param onBackToStorefront depois do pedido, volta para a vitrine limpando a pilha do pedido.
 * @param onOpenOrder "Acompanhar pedido" abre os detalhes do pedido feito.
 */
fun NavGraphBuilder.checkoutScreens(
    navController: NavController,
    onSignIn: () -> Unit,
    onBackToStorefront: () -> Unit,
    onOpenOrder: (orderId: String) -> Unit,
) {
    composable<CheckoutDestination> {
        CheckoutRoute(
            onBack = navController::popBackStack,
            onSignIn = onSignIn,
            onPlaced = { placed ->
                navController.navigate(
                    OrderPlacedDestination(placed.id, placed.number, placed.storeName, placed.total),
                ) {
                    popUpTo(CheckoutDestination) { inclusive = true }
                }
            },
        )
    }
    composable<OrderPlacedDestination> { entry ->
        val placed = entry.toRoute<OrderPlacedDestination>()
        OrderPlacedScreen(
            number = placed.number,
            storeName = placed.storeName,
            total = placed.total,
            onBackToStorefront = onBackToStorefront,
            onTrackOrder = { onOpenOrder(placed.orderId) },
        )
    }
}
