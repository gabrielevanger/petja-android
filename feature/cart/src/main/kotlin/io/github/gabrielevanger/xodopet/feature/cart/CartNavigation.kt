package io.github.gabrielevanger.xodopet.feature.cart

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object CartDestination

fun NavController.navigateToCart() = navigate(CartDestination) { launchSingleTop = true }

fun NavGraphBuilder.cartScreen(onBack: () -> Unit) {
    composable<CartDestination> { CartRoute(onBack = onBack) }
}
