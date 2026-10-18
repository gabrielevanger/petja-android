package io.github.gabrielevanger.xodopet.feature.storefront

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** Vitrine: a tela inicial do app. */
@Serializable
data object StorefrontDestination

fun NavGraphBuilder.storefrontScreen(
    onStoreClick: (storeId: String) -> Unit,
    onCartClick: () -> Unit,
    onAccountClick: () -> Unit,
) {
    composable<StorefrontDestination> {
        StorefrontRoute(onStoreClick = onStoreClick, onCartClick = onCartClick, onAccountClick = onAccountClick)
    }
}
