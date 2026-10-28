package io.github.gabrielevanger.petja.feature.account.signup

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gabrielevanger.petja.core.designsystem.component.FormField
import io.github.gabrielevanger.petja.core.designsystem.component.FormFieldType
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme
import io.github.gabrielevanger.petja.core.domain.DiagnosticLog
import io.github.gabrielevanger.petja.core.domain.account.AccountRepository
import io.github.gabrielevanger.petja.core.domain.account.AccountValidation
import io.github.gabrielevanger.petja.core.domain.account.FieldError
import io.github.gabrielevanger.petja.core.domain.account.SignUpResult
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
import io.github.gabrielevanger.petja.feature.account.message
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun SignUpRoute(onBack: () -> Unit, onSignedIn: () -> Unit, viewModel: SignUpViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            SignUpEvent.Close -> onBack()
            SignUpEvent.SignedIn -> onSignedIn()
        }
    }
    SignUpScreen(state, viewModel::onAction)
}

@Composable
fun SignUpScreen(state: FormUiState<SignUpResult>, onAction: (SignUpAction) -> Unit) {
    val onChange: (Field, String) -> Unit = { field, value -> onAction(SignUpAction.OnFieldChange(field, value)) }
    val onSubmit = { onAction(SignUpAction.OnSubmitClick) }
    val onBack = { onAction(SignUpAction.OnBackClick) }
    if (state.result == SignUpResult.ConfirmationEmailSent) {
        AccountScaffold(title = stringResource(R.string.sign_up_confirm_title), onBack = onBack) {
            Text(
                text = stringResource(R.string.sign_up_confirm_text, state.value(Field.Email).trim()),
                style = PetjaTheme.typography.body,
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
