package io.github.gabrielevanger.petja.feature.account.forgotpassword

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gabrielevanger.petja.core.designsystem.component.Notice
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme
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
fun ForgotPasswordRoute(onBack: () -> Unit, viewModel: ForgotPasswordViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            ForgotPasswordEvent.Close -> onBack()
        }
    }
    ForgotPasswordScreen(state, viewModel::onAction)
}

@Composable
fun ForgotPasswordScreen(state: FormUiState<Unit>, onAction: (ForgotPasswordAction) -> Unit) {
    val onChange: (
        Field,
        String,
    ) -> Unit = { field, value -> onAction(ForgotPasswordAction.OnFieldChange(field, value)) }
    val onSubmit = { onAction(ForgotPasswordAction.OnSubmitClick) }
    val onBack = { onAction(ForgotPasswordAction.OnBackClick) }
    AccountScaffold(title = stringResource(R.string.forgot_title), onBack = onBack) {
        if (state.result != null) {
            Notice(stringResource(R.string.forgot_sent, state.value(Field.Email).trim()))
            return@AccountScaffold
        }
        Text(text = stringResource(R.string.forgot_text), style = PetjaTheme.typography.body)
        EmailField(state, onChange, ImeAction.Done)
        FormError(state)
        SubmitButton(R.string.forgot_send, state.submitting, onSubmit)
    }
}
