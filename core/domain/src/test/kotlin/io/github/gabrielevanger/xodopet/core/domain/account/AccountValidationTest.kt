package io.github.gabrielevanger.xodopet.core.domain.account

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AccountValidationTest {
    @Test
    fun email() {
        assertEquals(FieldError.Required, AccountValidation.email("  "))
        assertEquals(FieldError.InvalidEmail, AccountValidation.email("ana@"))
        assertEquals(FieldError.InvalidEmail, AccountValidation.email("ana teste@x.com"))
        assertNull(AccountValidation.email(" ana@teste.com "))
    }

    @Test
    fun password() {
        assertEquals(FieldError.Required, AccountValidation.password(""))
        assertEquals(FieldError.PasswordTooShort, AccountValidation.password("1234567"))
        assertNull(AccountValidation.password("12345678"))
        assertEquals(FieldError.PasswordsDoNotMatch, AccountValidation.passwordConfirmation("12345678", "12345679"))
        assertNull(AccountValidation.passwordConfirmation("12345678", "12345678"))
    }

    @Test
    fun fullName() {
        assertEquals(FieldError.Required, AccountValidation.fullName(" "))
        assertEquals(FieldError.NameTooShort, AccountValidation.fullName("A "))
        assertNull(AccountValidation.fullName("Ana"))
    }
}
