package io.github.gabrielevanger.xodopet.feature.storemode

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object StoreModeDestination

fun NavController.navigateToStoreMode() = navigate(StoreModeDestination) { launchSingleTop = true }

fun NavGraphBuilder.storeModeScreen(navController: NavController, onSignIn: () -> Unit) {
    composable<StoreModeDestination> { StoreModeRoute(onBack = navController::popBackStack, onSignIn = onSignIn) }
}
