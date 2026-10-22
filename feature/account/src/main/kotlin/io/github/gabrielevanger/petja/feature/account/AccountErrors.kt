package io.github.gabrielevanger.petja.feature.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.gabrielevanger.petja.core.domain.account.AccountError
import io.github.gabrielevanger.petja.core.domain.account.FieldError
import io.github.gabrielevanger.petja.core.domain.account.MIN_PASSWORD_LENGTH

@Composable
internal fun AccountError.message(): String = stringResource(
    when (this) {
        AccountError.InvalidCredentials -> R.string.error_invalid_credentials
        AccountError.EmailNotConfirmed -> R.string.error_email_not_confirmed
        AccountError.EmailAlreadyInUse -> R.string.error_email_in_use
        AccountError.WeakPassword -> R.string.error_weak_password
        AccountError.SamePassword -> R.string.error_same_password
        AccountError.InvalidEmail -> R.string.error_invalid_email
        AccountError.TooManyEmails -> R.string.error_too_many_emails
        AccountError.Network -> R.string.error_network
        AccountError.Unknown -> R.string.error_unknown
    },
)

@Composable
internal fun FieldError?.message(): String? = when (this) {
    null -> null
    FieldError.Required -> stringResource(R.string.field_error_required)
    FieldError.InvalidEmail -> stringResource(R.string.field_error_email)
    FieldError.PasswordTooShort -> stringResource(R.string.field_error_password_short, MIN_PASSWORD_LENGTH)
    FieldError.PasswordsDoNotMatch -> stringResource(R.string.field_error_passwords_differ)
    FieldError.NameTooShort -> stringResource(R.string.field_error_name_short)
}
