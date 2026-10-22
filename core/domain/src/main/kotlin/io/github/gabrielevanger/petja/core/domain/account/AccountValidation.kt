package io.github.gabrielevanger.petja.core.domain.account

/** Mínimo de caracteres da senha. O Supabase aceita 6; o PetJá pede um pouco mais. */
const val MIN_PASSWORD_LENGTH = 8

/** Nome com pelo menos duas letras, para o entregador ter como chamar o cliente. */
private const val MIN_NAME_LENGTH = 2

private val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

enum class FieldError { Required, InvalidEmail, PasswordTooShort, PasswordsDoNotMatch, NameTooShort }

/** Conferências feitas antes de chamar o servidor, para a tela apontar o campo certo na hora. */
object AccountValidation {
    fun email(value: String): FieldError? = when {
        value.isBlank() -> FieldError.Required
        !EMAIL_PATTERN.matches(value.trim()) -> FieldError.InvalidEmail
        else -> null
    }

    fun password(value: String): FieldError? = when {
        value.isEmpty() -> FieldError.Required
        value.length < MIN_PASSWORD_LENGTH -> FieldError.PasswordTooShort
        else -> null
    }

    fun passwordConfirmation(password: String, confirmation: String): FieldError? =
        if (password != confirmation) FieldError.PasswordsDoNotMatch else null

    fun fullName(value: String): FieldError? = when {
        value.isBlank() -> FieldError.Required
        value.trim().length < MIN_NAME_LENGTH -> FieldError.NameTooShort
        else -> null
    }
}
