package io.github.gabrielevanger.xodopet.feature.account

import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gabrielevanger.xodopet.core.domain.DiagnosticLog
import io.github.gabrielevanger.xodopet.core.domain.account.AccountRepository
import io.github.gabrielevanger.xodopet.core.domain.account.AccountValidation
import io.github.gabrielevanger.xodopet.core.domain.account.FieldError
import io.github.gabrielevanger.xodopet.core.domain.account.SignUpResult
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(private val accounts: AccountRepository, log: DiagnosticLog) :
    AccountFormViewModel<Unit>(log) {
    override fun validate(values: Map<Field, String>): Map<Field, FieldError> = mapOf(
        Field.Email to AccountValidation.email(values[Field.Email].orEmpty()),
        // No login, a senha só precisa estar preenchida: o tamanho mínimo pode ter mudado desde o cadastro.
        Field.Password to if (values[Field.Password].isNullOrEmpty()) FieldError.Required else null,
    ).withoutValid()

    override suspend fun send(values: Map<Field, String>) =
        accounts.signIn(values.getValue(Field.Email), values.getValue(Field.Password))
}

@HiltViewModel
class SignUpViewModel @Inject constructor(private val accounts: AccountRepository, log: DiagnosticLog) :
    AccountFormViewModel<SignUpResult>(log) {
    override fun validate(values: Map<Field, String>): Map<Field, FieldError> = mapOf(
        Field.Name to AccountValidation.fullName(values[Field.Name].orEmpty()),
        Field.Email to AccountValidation.email(values[Field.Email].orEmpty()),
        Field.Password to AccountValidation.password(values[Field.Password].orEmpty()),
    ).withoutValid()

    override suspend fun send(values: Map<Field, String>) =
        accounts.signUp(values.getValue(Field.Name), values.getValue(Field.Email), values.getValue(Field.Password))
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(private val accounts: AccountRepository, log: DiagnosticLog) :
    AccountFormViewModel<Unit>(log) {
    override fun validate(values: Map<Field, String>): Map<Field, FieldError> =
        mapOf(Field.Email to AccountValidation.email(values[Field.Email].orEmpty())).withoutValid()

    override suspend fun send(values: Map<Field, String>) = accounts.sendPasswordReset(values.getValue(Field.Email))
}

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(private val accounts: AccountRepository, log: DiagnosticLog) :
    AccountFormViewModel<Unit>(log) {
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
}
