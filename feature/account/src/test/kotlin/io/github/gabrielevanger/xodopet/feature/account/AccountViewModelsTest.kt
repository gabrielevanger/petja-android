package io.github.gabrielevanger.xodopet.feature.account

import androidx.lifecycle.SavedStateHandle
import io.github.gabrielevanger.xodopet.core.domain.DiagnosticLog
import io.github.gabrielevanger.xodopet.core.domain.account.AccountError
import io.github.gabrielevanger.xodopet.core.domain.account.AccountState
import io.github.gabrielevanger.xodopet.core.domain.account.FieldError
import io.github.gabrielevanger.xodopet.core.domain.account.SignUpResult
import io.github.gabrielevanger.xodopet.core.domain.testing.ANA
import io.github.gabrielevanger.xodopet.core.domain.testing.FakeAccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val accounts = FakeAccountRepository()
    private val warnings = mutableListOf<String>()
    private val log = DiagnosticLog { message, _ -> warnings += message }

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `field errors only show after the first submit and follow the typing`() = runTest(dispatcher) {
        val viewModel = SignInViewModel(accounts, log)

        viewModel.onChange(Field.Email, "ana@")
        assertTrue(viewModel.state.value.fieldErrors.isEmpty(), "sem acusar enquanto digita")

        viewModel.submit()
        assertEquals(
            mapOf(Field.Email to FieldError.InvalidEmail, Field.Password to FieldError.Required),
            viewModel.state.value.fieldErrors,
        )

        viewModel.onChange(Field.Email, "ana@teste.local")
        assertEquals(mapOf(Field.Password to FieldError.Required), viewModel.state.value.fieldErrors)
        assertFalse(viewModel.state.value.submitting, "não chama o servidor com campo errado")
    }

    @Test
    fun `sign in succeeds`() = runTest(dispatcher) {
        val viewModel = SignInViewModel(accounts, log)
        viewModel.onChange(Field.Email, "ana@teste.local")
        viewModel.onChange(Field.Password, "x")

        viewModel.submit()
        assertTrue(viewModel.state.value.submitting)
        advanceUntilIdle()

        assertEquals(Unit, viewModel.state.value.result)
        assertTrue(accounts.state.value is AccountState.SignedIn)
    }

    @Test
    fun `server error is shown and cleared when typing again`() = runTest(dispatcher) {
        accounts.failWith = AccountError.InvalidCredentials
        val viewModel = SignInViewModel(accounts, log)
        viewModel.onChange(Field.Email, "ana@teste.local")
        viewModel.onChange(Field.Password, "errada123")

        viewModel.submit()
        advanceUntilIdle()
        assertEquals(AccountError.InvalidCredentials, viewModel.state.value.error)
        assertFalse(viewModel.state.value.submitting)
        assertTrue(warnings.isEmpty(), "erro esperado não vai para o log")

        viewModel.onChange(Field.Password, "certa1234")
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `network errors are logged`() = runTest(dispatcher) {
        accounts.failWith = AccountError.Network
        val viewModel = ForgotPasswordViewModel(accounts, log)
        viewModel.onChange(Field.Email, "ana@teste.local")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(AccountError.Network, viewModel.state.value.error)
        assertEquals(1, warnings.size)
    }

    @Test
    fun `double tap submits once`() = runTest(dispatcher) {
        val viewModel = ForgotPasswordViewModel(accounts, log)
        viewModel.onChange(Field.Email, "ana@teste.local")

        viewModel.submit()
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(listOf("ana@teste.local"), accounts.passwordResets)
    }

    @Test
    fun `sign up validates name, email and password`() = runTest(dispatcher) {
        val viewModel = SignUpViewModel(accounts, log)
        viewModel.onChange(Field.Password, "curta")

        viewModel.submit()

        assertEquals(
            mapOf(
                Field.Name to FieldError.Required,
                Field.Email to FieldError.Required,
                Field.Password to FieldError.PasswordTooShort,
            ),
            viewModel.state.value.fieldErrors,
        )
    }

    @Test
    fun `sign up can wait for the confirmation email`() = runTest(dispatcher) {
        accounts.signUpResult = SignUpResult.ConfirmationEmailSent
        val viewModel = SignUpViewModel(accounts, log)
        viewModel.onChange(Field.Name, "Bia")
        viewModel.onChange(Field.Email, "bia@teste.local")
        viewModel.onChange(Field.Password, "segredo123")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(SignUpResult.ConfirmationEmailSent, viewModel.state.value.result)
        assertEquals(AccountState.SignedOut, accounts.state.value)
    }

    @Test
    fun `reset password needs both fields equal`() = runTest(dispatcher) {
        val viewModel = ResetPasswordViewModel(accounts, log)
        viewModel.onChange(Field.Password, "novasenha123")
        viewModel.onChange(Field.PasswordConfirmation, "novasenha124")

        viewModel.submit()
        assertEquals(
            mapOf(Field.PasswordConfirmation to FieldError.PasswordsDoNotMatch),
            viewModel.state.value.fieldErrors,
        )

        viewModel.onChange(Field.PasswordConfirmation, "novasenha123")
        viewModel.submit()
        advanceUntilIdle()
        assertEquals("novasenha123", accounts.lastPassword)
    }

    @Test
    fun `account screen state, notice and sign out`() = runTest(dispatcher) {
        val signedIn = FakeAccountRepository(AccountState.SignedIn(ANA))
        val viewModel = AccountViewModel(SavedStateHandle(mapOf(NOTICE_ARG to "EmailConfirmed")), signedIn, log)
        viewModel.state.launchIn(backgroundScope)
        advanceUntilIdle()

        assertEquals(AccountNotice.EmailConfirmed, viewModel.notice)
        assertEquals(AccountState.SignedIn(ANA), viewModel.state.value)

        viewModel.signOut()
        advanceUntilIdle()
        assertEquals(AccountState.SignedOut, viewModel.state.value)
    }

    @Test
    fun `sign out failure is only logged`() = runTest(dispatcher) {
        val signedIn = FakeAccountRepository(AccountState.SignedIn(ANA)).apply { failWith = AccountError.Network }
        val viewModel = AccountViewModel(SavedStateHandle(), signedIn, log)

        viewModel.signOut()
        advanceUntilIdle()

        assertNull(viewModel.notice)
        assertEquals(1, warnings.size)
    }
}
