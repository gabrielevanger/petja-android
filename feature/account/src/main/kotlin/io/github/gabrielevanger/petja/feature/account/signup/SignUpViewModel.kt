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

@HiltViewModel
class SignUpViewModel @Inject constructor(private val accounts: AccountRepository, log: DiagnosticLog) :
    AccountFormViewModel<SignUpResult>(log) {
    private val _events = Channel<SignUpEvent>(Channel.BUFFERED)
    val events: Flow<SignUpEvent> = _events.receiveAsFlow()

    fun onAction(action: SignUpAction) {
        when (action) {
            is SignUpAction.OnFieldChange -> change(action.field, action.value)
            SignUpAction.OnSubmitClick -> submit()
            SignUpAction.OnBackClick -> _events.trySend(SignUpEvent.Close)
        }
    }

    override fun validate(values: Map<Field, String>): Map<Field, FieldError> = mapOf(
        Field.Name to AccountValidation.fullName(values[Field.Name].orEmpty()),
        Field.Email to AccountValidation.email(values[Field.Email].orEmpty()),
        Field.Password to AccountValidation.password(values[Field.Password].orEmpty()),
    ).withoutValid()

    override suspend fun send(values: Map<Field, String>) =
        accounts.signUp(values.getValue(Field.Name), values.getValue(Field.Email), values.getValue(Field.Password))

    /** Com confirmação por e-mail, o resultado fica no estado e a tela mostra o aviso. */
    override fun onSuccess(result: SignUpResult) {
        if (result == SignUpResult.SignedIn) _events.trySend(SignUpEvent.SignedIn)
    }
}
