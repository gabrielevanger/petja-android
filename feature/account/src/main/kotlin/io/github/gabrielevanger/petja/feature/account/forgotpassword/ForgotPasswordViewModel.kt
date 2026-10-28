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

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(private val accounts: AccountRepository, log: DiagnosticLog) :
    AccountFormViewModel<Unit>(log) {
    private val _events = Channel<ForgotPasswordEvent>(Channel.BUFFERED)
    val events: Flow<ForgotPasswordEvent> = _events.receiveAsFlow()

    fun onAction(action: ForgotPasswordAction) {
        when (action) {
            is ForgotPasswordAction.OnFieldChange -> change(action.field, action.value)
            ForgotPasswordAction.OnSubmitClick -> submit()
            ForgotPasswordAction.OnBackClick -> _events.trySend(ForgotPasswordEvent.Close)
        }
    }

    override fun validate(values: Map<Field, String>): Map<Field, FieldError> =
        mapOf(Field.Email to AccountValidation.email(values[Field.Email].orEmpty())).withoutValid()

    /** Enviado, o resultado fica no estado e a tela mostra o aviso de e-mail enviado. */
    override suspend fun send(values: Map<Field, String>) = accounts.sendPasswordReset(values.getValue(Field.Email))
}
