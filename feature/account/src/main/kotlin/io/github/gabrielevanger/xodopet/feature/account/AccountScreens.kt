package io.github.gabrielevanger.xodopet.feature.account

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gabrielevanger.xodopet.core.designsystem.component.FormField
import io.github.gabrielevanger.xodopet.core.designsystem.component.FormFieldType
import io.github.gabrielevanger.xodopet.core.designsystem.component.LoadingState
import io.github.gabrielevanger.xodopet.core.designsystem.component.Notice
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme
import io.github.gabrielevanger.xodopet.core.domain.account.AccountState
import io.github.gabrielevanger.xodopet.core.domain.account.SignUpResult
import io.github.gabrielevanger.xodopet.core.ui.BackTopBar

// ---------------------------------------------------------------------------------------------
// Conta
// ---------------------------------------------------------------------------------------------

@Composable
fun AccountRoute(
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onOrders: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AccountScreen(
        state,
        viewModel.notice,
        onBack,
        onSignIn,
        onSignUp,
        onSignOut = viewModel::signOut,
        onOrders = onOrders,
    )
}

@Composable
fun AccountScreen(
    state: AccountState,
    notice: AccountNotice?,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onSignOut: () -> Unit,
    onOrders: () -> Unit = {},
) {
    AccountScaffold(title = stringResource(R.string.account_title), onBack = onBack) {
        notice?.let { Notice(stringResource(it.messageRes())) }
        when (state) {
            AccountState.Loading -> LoadingState(stringResource(R.string.account_loading))

            AccountState.SignedOut -> {
                Text(text = stringResource(R.string.account_signed_out_text), style = XodoTheme.typography.body)
                Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.account_sign_in))
                }
                OutlinedButton(onClick = onSignUp, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.account_sign_up))
                }
            }

            is AccountState.SignedIn -> {
                Text(
                    text = state.account.fullName?.let { stringResource(R.string.account_hello, it) }
                        ?: stringResource(R.string.account_hello_no_name),
                    style = XodoTheme.typography.sectionTitle,
                )
                Text(
                    text = state.account.email,
                    style = XodoTheme.typography.body,
                    color = XodoTheme.colors.onSurfaceVariant,
                )
                Button(onClick = onOrders, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.account_orders))
                }
                OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.account_sign_out))
                }
            }
        }
    }
}

private fun AccountNotice.messageRes() = when (this) {
    AccountNotice.EmailConfirmed -> R.string.account_notice_email_confirmed
    AccountNotice.LinkExpired -> R.string.account_notice_link_expired
    AccountNotice.PasswordChanged -> R.string.account_notice_password_changed
}

// ---------------------------------------------------------------------------------------------
// Entrar
// ---------------------------------------------------------------------------------------------

@Composable
fun SignInRoute(
    onBack: () -> Unit,
    onSignedIn: () -> Unit,
    onForgotPassword: () -> Unit,
    onSignUp: () -> Unit,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val signedIn = state.result != null
    LaunchedEffect(signedIn) { if (signedIn) onSignedIn() }
    SignInScreen(state, onBack, viewModel::onChange, viewModel::submit, onForgotPassword, onSignUp)
}

@Composable
fun SignInScreen(
    state: FormUiState<Unit>,
    onBack: () -> Unit,
    onChange: (Field, String) -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    onSignUp: () -> Unit,
) {
    AccountScaffold(title = stringResource(R.string.sign_in_title), onBack = onBack) {
        EmailField(state, onChange)
        PasswordField(state, onChange, Field.Password, R.string.field_password, ImeAction.Done)
        FormError(state)
        SubmitButton(R.string.account_sign_in, state.submitting, onSubmit)
        TextButton(onClick = onForgotPassword) { Text(stringResource(R.string.sign_in_forgot)) }
        TextButton(onClick = onSignUp) { Text(stringResource(R.string.sign_in_no_account)) }
    }
}

// ---------------------------------------------------------------------------------------------
// Criar conta
// ---------------------------------------------------------------------------------------------

@Composable
fun SignUpRoute(onBack: () -> Unit, onSignedIn: () -> Unit, viewModel: SignUpViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.result) { if (state.result == SignUpResult.SignedIn) onSignedIn() }
    SignUpScreen(state, onBack, viewModel::onChange, viewModel::submit)
}

@Composable
fun SignUpScreen(
    state: FormUiState<SignUpResult>,
    onBack: () -> Unit,
    onChange: (Field, String) -> Unit,
    onSubmit: () -> Unit,
) {
    if (state.result == SignUpResult.ConfirmationEmailSent) {
        AccountScaffold(title = stringResource(R.string.sign_up_confirm_title), onBack = onBack) {
            Text(
                text = stringResource(R.string.sign_up_confirm_text, state.value(Field.Email).trim()),
                style = XodoTheme.typography.body,
            )
        }
        return
    }
    AccountScaffold(title = stringResource(R.string.sign_up_title), onBack = onBack) {
        FormField(
            value = state.value(Field.Name),
            onValueChange = { onChange(Field.Name, it) },
            label = stringResource(R.string.field_name),
            type = FormFieldType.Name,
            error = state.fieldErrors[Field.Name].message(),
            enabled = !state.submitting,
        )
        EmailField(state, onChange)
        PasswordField(state, onChange, Field.Password, R.string.field_password, ImeAction.Done)
        FormError(state)
        SubmitButton(R.string.account_sign_up, state.submitting, onSubmit)
    }
}

// ---------------------------------------------------------------------------------------------
// Esqueci a senha e senha nova
// ---------------------------------------------------------------------------------------------

@Composable
fun ForgotPasswordRoute(onBack: () -> Unit, viewModel: ForgotPasswordViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ForgotPasswordScreen(state, onBack, viewModel::onChange, viewModel::submit)
}

@Composable
fun ForgotPasswordScreen(
    state: FormUiState<Unit>,
    onBack: () -> Unit,
    onChange: (Field, String) -> Unit,
    onSubmit: () -> Unit,
) {
    AccountScaffold(title = stringResource(R.string.forgot_title), onBack = onBack) {
        if (state.result != null) {
            Notice(stringResource(R.string.forgot_sent, state.value(Field.Email).trim()))
            return@AccountScaffold
        }
        Text(text = stringResource(R.string.forgot_text), style = XodoTheme.typography.body)
        EmailField(state, onChange, ImeAction.Done)
        FormError(state)
        SubmitButton(R.string.forgot_send, state.submitting, onSubmit)
    }
}

@Composable
fun ResetPasswordRoute(onBack: () -> Unit, onDone: () -> Unit, viewModel: ResetPasswordViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val done = state.result != null
    LaunchedEffect(done) { if (done) onDone() }
    ResetPasswordScreen(state, onBack, viewModel::onChange, viewModel::submit)
}

@Composable
fun ResetPasswordScreen(
    state: FormUiState<Unit>,
    onBack: () -> Unit,
    onChange: (Field, String) -> Unit,
    onSubmit: () -> Unit,
) {
    AccountScaffold(title = stringResource(R.string.reset_title), onBack = onBack) {
        PasswordField(state, onChange, Field.Password, R.string.field_new_password, ImeAction.Next)
        PasswordField(state, onChange, Field.PasswordConfirmation, R.string.field_password_confirmation, ImeAction.Done)
        FormError(state)
        SubmitButton(R.string.reset_save, state.submitting, onSubmit)
    }
}

// ---------------------------------------------------------------------------------------------
// Peças comuns
// ---------------------------------------------------------------------------------------------

@Composable
private fun AccountScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(containerColor = XodoTheme.colors.background, topBar = { BackTopBar(title, onBack) }) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(XodoTheme.spacing.s),
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(XodoTheme.spacing.m),
            content = content,
        )
    }
}

@Composable
private fun EmailField(
    state: FormUiState<*>,
    onChange: (Field, String) -> Unit,
    imeAction: ImeAction = ImeAction.Next,
) {
    FormField(
        value = state.value(Field.Email),
        onValueChange = { onChange(Field.Email, it) },
        label = stringResource(R.string.field_email),
        type = FormFieldType.Email,
        error = state.fieldErrors[Field.Email].message(),
        enabled = !state.submitting,
        imeAction = imeAction,
    )
}

@Composable
private fun PasswordField(
    state: FormUiState<*>,
    onChange: (Field, String) -> Unit,
    field: Field,
    @StringRes label: Int,
    imeAction: ImeAction,
) {
    FormField(
        value = state.value(field),
        onValueChange = { onChange(field, it) },
        label = stringResource(label),
        type = FormFieldType.Password,
        error = state.fieldErrors[field].message(),
        enabled = !state.submitting,
        imeAction = imeAction,
    )
}

@Composable
private fun FormError(state: FormUiState<*>) {
    state.error?.let {
        Text(text = it.message(), style = XodoTheme.typography.body, color = XodoTheme.colors.error)
    }
}

@Composable
private fun SubmitButton(@StringRes label: Int, submitting: Boolean, onSubmit: () -> Unit) {
    Button(onClick = onSubmit, enabled = !submitting, modifier = Modifier.fillMaxWidth()) {
        if (submitting) {
            CircularProgressIndicator(
                modifier = Modifier.padding(end = XodoTheme.spacing.xs).size(XodoTheme.sizes.inlineProgress),
                strokeWidth = XodoTheme.sizes.divider * 2,
            )
        }
        Text(stringResource(label))
    }
}
