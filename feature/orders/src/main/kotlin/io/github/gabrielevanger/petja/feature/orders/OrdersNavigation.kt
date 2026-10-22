package io.github.gabrielevanger.petja.feature.orders

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object OrdersDestination

/** O nome do campo é a chave do argumento no SavedStateHandle. */
@Serializable
data class OrderDetailsDestination(val orderId: String)

internal const val ORDER_ID_ARG = "orderId"

fun NavController.navigateToOrders() = navigate(OrdersDestination) { launchSingleTop = true }

fun NavController.navigateToOrder(orderId: String) = navigate(OrderDetailsDestination(orderId))

/** @param onOpenCart depois de "Pedir de novo", o app abre o carrinho. */
fun NavGraphBuilder.ordersScreens(navController: NavController, onSignIn: () -> Unit, onOpenCart: () -> Unit) {
    composable<OrdersDestination> {
        OrdersRoute(
            onBack = navController::popBackStack,
            onOpenOrder = navController::navigateToOrder,
            onSignIn = onSignIn,
        )
    }
    composable<OrderDetailsDestination> {
        OrderDetailsRoute(onBack = navController::popBackStack, onOpenCart = onOpenCart)
    }
}
