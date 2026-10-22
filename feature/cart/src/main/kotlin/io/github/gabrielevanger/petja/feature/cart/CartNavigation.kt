package io.github.gabrielevanger.petja.feature.cart

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object CartDestination

fun NavController.navigateToCart() = navigate(CartDestination) { launchSingleTop = true }

fun NavGraphBuilder.cartScreen(onBack: () -> Unit, onCheckout: () -> Unit, onSignIn: () -> Unit) {
    composable<CartDestination> { CartRoute(onBack = onBack, onCheckout = onCheckout, onSignIn = onSignIn) }
}
