package io.github.gabrielevanger.xodopet.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme
import io.github.gabrielevanger.xodopet.feature.cart.cartScreen
import io.github.gabrielevanger.xodopet.feature.cart.navigateToCart
import io.github.gabrielevanger.xodopet.feature.store.navigateToStore
import io.github.gabrielevanger.xodopet.feature.store.storeScreen
import io.github.gabrielevanger.xodopet.feature.storefront.StorefrontDestination
import io.github.gabrielevanger.xodopet.feature.storefront.storefrontScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Desenha atrás das barras do sistema; as telas usam safeDrawingPadding.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            XodoTheme {
                val navController = rememberNavController()
                // Cada feature declara as próprias rotas; o app só liga uma tela à outra.
                NavHost(navController = navController, startDestination = StorefrontDestination) {
                    storefrontScreen(
                        onStoreClick = navController::navigateToStore,
                        onCartClick = navController::navigateToCart,
                    )
                    storeScreen(onBack = navController::popBackStack, onCartClick = navController::navigateToCart)
                    cartScreen(onBack = navController::popBackStack)
                }
            }
        }
    }
}
