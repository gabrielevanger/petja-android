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

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(private val accounts: AccountRepository, log: DiagnosticLog) :
    AccountFormViewModel<Unit>(log) {
    private val _events = Channel<ResetPasswordEvent>(Channel.BUFFERED)
    val events: Flow<ResetPasswordEvent> = _events.receiveAsFlow()

    fun onAction(action: ResetPasswordAction) {
        when (action) {
            is ResetPasswordAction.OnFieldChange -> change(action.field, action.value)
            ResetPasswordAction.OnSubmitClick -> submit()
            ResetPasswordAction.OnBackClick -> _events.trySend(ResetPasswordEvent.Close)
        }
    }

    override fun validate(values: Map<Field, String>): Map<Field, FieldError> {
        val password = values[Field.Password].orEmpty()
        return mapOf(
            Field.Password to AccountValidation.password(password),
            Field.PasswordConfirmation to AccountValidation.passwordConfirmation(
                password,
                values[Field.PasswordConfirmation].orEmpty(),
            ),
        ).withoutValid()
    }

    override suspend fun send(values: Map<Field, String>) = accounts.updatePassword(values.getValue(Field.Password))

    override fun onSuccess(result: Unit) {
        _events.trySend(ResetPasswordEvent.Done)
    }
}
