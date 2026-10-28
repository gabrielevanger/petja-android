package io.github.gabrielevanger.petja.feature.account.signin

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gabrielevanger.petja.core.domain.DiagnosticLog
import io.github.gabrielevanger.petja.core.domain.account.AccountRepository
import io.github.gabrielevanger.petja.core.domain.account.AccountValidation
import io.github.gabrielevanger.petja.core.domain.account.FieldError
import io.github.gabrielevanger.petja.core.ui.CollectEvents
import io.github.gabrielevanger.petja.feature.account.R
import io.github.gabrielevanger.petja.feature.account.form.AccountFormViewModel
import io.github.gabrielevanger.petja.feature.account.form.AccountScaffold
import io.github.gabrielevanger.petja.feature.account.form.EmailField
import io.github.gabrielevanger.petja.feature.account.form.Field
import io.github.gabrielevanger.petja.feature.account.form.FormError
import io.github.gabrielevanger.petja.feature.account.form.FormUiState
import io.github.gabrielevanger.petja.feature.account.form.PasswordField
import io.github.gabrielevanger.petja.feature.account.form.SubmitButton
import io.github.gabrielevanger.petja.feature.account.form.withoutValid
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun SignInRoute(
    onBack: () -> Unit,
    onSignedIn: () -> Unit,
    onForgotPassword: () -> Unit,
    onSignUp: () -> Unit,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            SignInEvent.Close -> onBack()
            SignInEvent.SignedIn -> onSignedIn()
            SignInEvent.OpenForgotPassword -> onForgotPassword()
            SignInEvent.OpenSignUp -> onSignUp()
        }
    }
    SignInScreen(state, viewModel::onAction)
}

@Composable
fun SignInScreen(state: FormUiState<Unit>, onAction: (SignInAction) -> Unit) {
    val onChange: (Field, String) -> Unit = { field, value -> onAction(SignInAction.OnFieldChange(field, value)) }
    val onSubmit = { onAction(SignInAction.OnSubmitClick) }
    val onBack = { onAction(SignInAction.OnBackClick) }
    AccountScaffold(title = stringResource(R.string.sign_in_title), onBack = onBack) {
        EmailField(state, onChange)
        PasswordField(state, onChange, Field.Password, R.string.field_password, ImeAction.Done)
        FormError(state)
        SubmitButton(R.string.account_sign_in, state.submitting, onSubmit)
        TextButton(onClick = {
            onAction(SignInAction.OnForgotPasswordClick)
        }) { Text(stringResource(R.string.sign_in_forgot)) }
        TextButton(onClick = {
            onAction(SignInAction.OnSignUpClick)
        }) { Text(stringResource(R.string.sign_in_no_account)) }
    }
}
