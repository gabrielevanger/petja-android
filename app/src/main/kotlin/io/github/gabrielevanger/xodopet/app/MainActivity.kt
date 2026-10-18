package io.github.gabrielevanger.xodopet.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme
import io.github.gabrielevanger.xodopet.feature.account.accountScreens
import io.github.gabrielevanger.xodopet.feature.account.navigateToAccount
import io.github.gabrielevanger.xodopet.feature.account.navigateToResetPassword
import io.github.gabrielevanger.xodopet.feature.account.navigateToSignIn
import io.github.gabrielevanger.xodopet.feature.cart.cartScreen
import io.github.gabrielevanger.xodopet.feature.cart.navigateToCart
import io.github.gabrielevanger.xodopet.feature.checkout.checkoutScreens
import io.github.gabrielevanger.xodopet.feature.checkout.navigateToCheckout
import io.github.gabrielevanger.xodopet.feature.orders.navigateToOrder
import io.github.gabrielevanger.xodopet.feature.orders.navigateToOrders
import io.github.gabrielevanger.xodopet.feature.orders.ordersScreens
import io.github.gabrielevanger.xodopet.feature.store.navigateToStore
import io.github.gabrielevanger.xodopet.feature.store.storeScreen
import io.github.gabrielevanger.xodopet.feature.storefront.StorefrontDestination
import io.github.gabrielevanger.xodopet.feature.storefront.storefrontScreen
import io.github.gabrielevanger.xodopet.feature.storemode.navigateToStoreMode
import io.github.gabrielevanger.xodopet.feature.storemode.storeModeScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Desenha atrás das barras do sistema; as telas cuidam dos próprios espaços.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Link de e-mail que abriu o app; ao recriar a Activity (rotação), o link já foi tratado.
        if (savedInstanceState == null) appViewModel.onLink(intent?.dataString)
        setContent {
            XodoTheme {
                val navController = rememberNavController()
                LaunchedEffect(Unit) {
                    appViewModel.navigation.collect { destination ->
                        when (destination) {
                            AppNavigation.ResetPassword -> navController.navigateToResetPassword()
                            is AppNavigation.Account -> navController.navigateToAccount(destination.notice)
                        }
                    }
                }
                // Cada feature declara as próprias rotas; o app só liga uma tela à outra.
                NavHost(navController = navController, startDestination = StorefrontDestination) {
                    storefrontScreen(
                        onStoreClick = navController::navigateToStore,
                        onCartClick = navController::navigateToCart,
                        onAccountClick = { navController.navigateToAccount() },
                    )
                    storeScreen(onBack = navController::popBackStack, onCartClick = navController::navigateToCart)
                    cartScreen(
                        onBack = navController::popBackStack,
                        onCheckout = navController::navigateToCheckout,
                        onSignIn = navController::navigateToSignIn,
                    )
                    checkoutScreens(
                        navController,
                        onSignIn = navController::navigateToSignIn,
                        onBackToStorefront = { navController.popBackStack(StorefrontDestination, inclusive = false) },
                        onOpenOrder = { orderId ->
                            navController.popBackStack(StorefrontDestination, inclusive = false)
                            navController.navigateToOrder(orderId)
                        },
                    )
                    accountScreens(
                        navController,
                        onOrders = navController::navigateToOrders,
                        onStoreMode = navController::navigateToStoreMode,
                    )
                    storeModeScreen(navController, onSignIn = navController::navigateToSignIn)
                    ordersScreens(
                        navController,
                        onSignIn = navController::navigateToSignIn,
                        onOpenCart = navController::navigateToCart,
                    )
                }
            }
        }
    }

    /** Link aberto com o app já na tela (launchMode singleTop). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        appViewModel.onLink(intent.dataString)
    }
}
