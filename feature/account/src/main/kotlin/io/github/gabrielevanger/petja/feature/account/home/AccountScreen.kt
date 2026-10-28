package io.github.gabrielevanger.petja.feature.account.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gabrielevanger.petja.core.designsystem.R as DesignR
import io.github.gabrielevanger.petja.core.designsystem.component.Avatar
import io.github.gabrielevanger.petja.core.designsystem.component.LoadingState
import io.github.gabrielevanger.petja.core.designsystem.component.PetjaButton
import io.github.gabrielevanger.petja.core.designsystem.component.PetjaButtonStyle
import io.github.gabrielevanger.petja.core.designsystem.component.PetjaCard
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme
import io.github.gabrielevanger.petja.core.designsystem.theme.highlight
import io.github.gabrielevanger.petja.core.designsystem.theme.highlightContainer
import io.github.gabrielevanger.petja.core.designsystem.theme.onHighlight
import io.github.gabrielevanger.petja.core.domain.account.AccountState
import io.github.gabrielevanger.petja.core.ui.CollectEvents
import io.github.gabrielevanger.petja.feature.account.AccountNotice
import io.github.gabrielevanger.petja.feature.account.R
import io.github.gabrielevanger.petja.feature.account.form.AccountScaffold

@Composable
fun AccountRoute(
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onOrders: () -> Unit,
    onStoreMode: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            AccountEvent.Close -> onBack()
            AccountEvent.OpenSignIn -> onSignIn()
            AccountEvent.OpenSignUp -> onSignUp()
            AccountEvent.OpenOrders -> onOrders()
            AccountEvent.OpenStoreMode -> onStoreMode()
        }
    }
    AccountScreen(state, viewModel::onAction)
}

@Composable
fun AccountScreen(state: AccountUiState, onAction: (AccountAction) -> Unit) {
    AccountScaffold(title = stringResource(R.string.account_title), onBack = { onAction(AccountAction.OnBackClick) }) {
        state.notice?.let { ConfirmationBanner(stringResource(it.messageRes())) }
        when (val account = state.account) {
            AccountState.Loading -> LoadingState(stringResource(R.string.account_loading))

            AccountState.SignedOut -> {
                Text(text = stringResource(R.string.account_signed_out_text), style = PetjaTheme.typography.body)
                PetjaButton(
                    text = stringResource(R.string.account_sign_in),
                    onClick = { onAction(AccountAction.OnSignInClick) },
                    modifier = Modifier.fillMaxWidth(),
                )
                PetjaButton(
                    text = stringResource(R.string.account_sign_up),
                    onClick = { onAction(AccountAction.OnSignUpClick) },
                    style = PetjaButtonStyle.Outlined,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is AccountState.SignedIn -> {
                ProfileCard(
                    name = account.account.fullName?.let { stringResource(R.string.account_hello, it) }
                        ?: stringResource(R.string.account_hello_no_name),
                    email = account.account.email,
                )
                IconAction(DesignR.drawable.ic_receipt_text) {
                    PetjaButton(
                        text = stringResource(R.string.account_orders),
                        onClick = { onAction(AccountAction.OnOrdersClick) },
                        style = PetjaButtonStyle.Tonal,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (state.canOperateStore) {
                    IconAction(DesignR.drawable.ic_store) {
                        PetjaButton(
                            text = stringResource(R.string.account_store_mode),
                            onClick = { onAction(AccountAction.OnStoreModeClick) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                IconAction(DesignR.drawable.ic_log_out) {
                    PetjaButton(
                        text = stringResource(R.string.account_sign_out),
                        onClick = { onAction(AccountAction.OnSignOutClick) },
                        style = PetjaButtonStyle.Outlined,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** Aviso positivo no topo da conta, como "e-mail confirmado". */
@Composable
private fun ConfirmationBanner(text: String) {
    PetjaCard(color = PetjaTheme.colors.highlightContainer) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PetjaTheme.spacing.s),
        ) {
            Icon(
                painter = painterResource(DesignR.drawable.ic_badge_check),
                contentDescription = null,
                tint = PetjaTheme.colors.highlight,
                modifier = Modifier.size(PetjaTheme.sizes.icon),
            )
            Text(text = text, style = PetjaTheme.typography.body, color = PetjaTheme.colors.onSurfaceVariant)
        }
    }
}

/** Cartão verde do perfil: avatar, saudação e e-mail. */
@Composable
private fun ProfileCard(name: String, email: String) {
    PetjaCard(
        color = PetjaTheme.colors.highlight,
        shape = PetjaTheme.shapes.panel,
        contentPadding = PetjaTheme.spacing.l,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PetjaTheme.spacing.s),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Avatar()
            Text(
                text = name,
                style = PetjaTheme.typography.headline,
                color = PetjaTheme.colors.onHighlight,
                textAlign = TextAlign.Center,
            )
            Text(text = email, style = PetjaTheme.typography.body, color = PetjaTheme.colors.onHighlight)
        }
    }
}

/** Botão com um ícone de traço à esquerda, como "Meus pedidos" e "Sair da conta" no layout. */
@Composable
private fun IconAction(@DrawableRes icon: Int, button: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PetjaTheme.spacing.s),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = PetjaTheme.colors.highlight,
            modifier = Modifier.size(PetjaTheme.sizes.icon),
        )
        Column(modifier = Modifier.weight(1f)) { button() }
    }
}

private fun AccountNotice.messageRes() = when (this) {
    AccountNotice.EmailConfirmed -> R.string.account_notice_email_confirmed
    AccountNotice.LinkExpired -> R.string.account_notice_link_expired
    AccountNotice.PasswordChanged -> R.string.account_notice_password_changed
}
