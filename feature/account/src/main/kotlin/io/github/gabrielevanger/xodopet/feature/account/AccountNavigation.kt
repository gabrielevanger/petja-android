package io.github.gabrielevanger.xodopet.feature.account

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** Aviso mostrado no topo da tela da conta ao chegar por um link de e-mail ou depois de trocar a senha. */
enum class AccountNotice { EmailConfirmed, LinkExpired, PasswordChanged }

/** O aviso vai pelo nome, para a rota usar só tipos simples. */
@Serializable
data class AccountDestination(val notice: String? = null)

internal const val NOTICE_ARG = "notice"

@Serializable
data object SignInDestination

@Serializable
data object SignUpDestination

@Serializable
data object ForgotPasswordDestination

@Serializable
data object ResetPasswordDestination

fun NavController.navigateToAccount(notice: AccountNotice? = null) = navigate(AccountDestination(notice?.name)) {
    launchSingleTop = true
}

fun NavController.navigateToSignIn() = navigate(SignInDestination) { launchSingleTop = true }

fun NavController.navigateToResetPassword() = navigate(ResetPasswordDestination) { launchSingleTop = true }

/** Telas de conta. Depois de entrar ou cadastrar, volta para quem pediu o login (a conta ou o carrinho). */
fun NavGraphBuilder.accountScreens(navController: NavController) {
    composable<AccountDestination> {
        AccountRoute(
            onBack = navController::popBackStack,
            onSignIn = { navController.navigate(SignInDestination) },
            onSignUp = { navController.navigate(SignUpDestination) },
        )
    }
    composable<SignInDestination> {
        SignInRoute(
            onBack = navController::popBackStack,
            onSignedIn = navController::popBackStack,
            onForgotPassword = { navController.navigate(ForgotPasswordDestination) },
            onSignUp = {
                navController.navigate(SignUpDestination) { popUpTo(SignInDestination) { inclusive = true } }
            },
        )
    }
    composable<SignUpDestination> {
        SignUpRoute(onBack = navController::popBackStack, onSignedIn = navController::popBackStack)
    }
    composable<ForgotPasswordDestination> { ForgotPasswordRoute(onBack = navController::popBackStack) }
    composable<ResetPasswordDestination> {
        ResetPasswordRoute(
            onBack = navController::popBackStack,
            onDone = {
                navController.navigate(AccountDestination(AccountNotice.PasswordChanged.name)) {
                    popUpTo(ResetPasswordDestination) { inclusive = true }
                }
            },
        )
    }
}
