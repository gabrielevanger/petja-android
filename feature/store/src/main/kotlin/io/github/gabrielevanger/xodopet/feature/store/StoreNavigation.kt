package io.github.gabrielevanger.xodopet.feature.store

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** Página de uma loja. O nome do campo é a chave do argumento no SavedStateHandle. */
@Serializable
data class StoreDestination(val storeId: String)

internal const val STORE_ID_ARG = "storeId"

fun NavController.navigateToStore(storeId: String) = navigate(StoreDestination(storeId))

fun NavGraphBuilder.storeScreen(onBack: () -> Unit, onCartClick: () -> Unit) {
    composable<StoreDestination> { StoreRoute(onBack = onBack, onCartClick = onCartClick) }
}
