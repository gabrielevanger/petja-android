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

@HiltViewModel
class SignInViewModel @Inject constructor(private val accounts: AccountRepository, log: DiagnosticLog) :
    AccountFormViewModel<Unit>(log) {
    private val _events = Channel<SignInEvent>(Channel.BUFFERED)
    val events: Flow<SignInEvent> = _events.receiveAsFlow()

    fun onAction(action: SignInAction) {
        when (action) {
            is SignInAction.OnFieldChange -> change(action.field, action.value)
            SignInAction.OnSubmitClick -> submit()
            SignInAction.OnBackClick -> _events.trySend(SignInEvent.Close)
            SignInAction.OnForgotPasswordClick -> _events.trySend(SignInEvent.OpenForgotPassword)
            SignInAction.OnSignUpClick -> _events.trySend(SignInEvent.OpenSignUp)
        }
    }

    override fun validate(values: Map<Field, String>): Map<Field, FieldError> = mapOf(
        Field.Email to AccountValidation.email(values[Field.Email].orEmpty()),
        // No login, a senha só precisa estar preenchida: o tamanho mínimo pode ter mudado desde o cadastro.
        Field.Password to if (values[Field.Password].isNullOrEmpty()) FieldError.Required else null,
    ).withoutValid()

    override suspend fun send(values: Map<Field, String>) =
        accounts.signIn(values.getValue(Field.Email), values.getValue(Field.Password))

    override fun onSuccess(result: Unit) {
        _events.trySend(SignInEvent.SignedIn)
    }
}
