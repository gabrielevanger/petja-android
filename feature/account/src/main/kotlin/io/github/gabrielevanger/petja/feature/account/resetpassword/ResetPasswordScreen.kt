package io.github.gabrielevanger.petja.feature.account.resetpassword

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
fun ResetPasswordRoute(onBack: () -> Unit, onDone: () -> Unit, viewModel: ResetPasswordViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            ResetPasswordEvent.Close -> onBack()
            ResetPasswordEvent.Done -> onDone()
        }
    }
    ResetPasswordScreen(state, viewModel::onAction)
}

@Composable
fun ResetPasswordScreen(state: FormUiState<Unit>, onAction: (ResetPasswordAction) -> Unit) {
    val onChange: (
        Field,
        String,
    ) -> Unit = { field, value -> onAction(ResetPasswordAction.OnFieldChange(field, value)) }
    val onSubmit = { onAction(ResetPasswordAction.OnSubmitClick) }
    val onBack = { onAction(ResetPasswordAction.OnBackClick) }
    AccountScaffold(title = stringResource(R.string.reset_title), onBack = onBack) {
        PasswordField(state, onChange, Field.Password, R.string.field_new_password, ImeAction.Next)
        PasswordField(state, onChange, Field.PasswordConfirmation, R.string.field_password_confirmation, ImeAction.Done)
        FormError(state)
        SubmitButton(R.string.reset_save, state.submitting, onSubmit)
    }
}
