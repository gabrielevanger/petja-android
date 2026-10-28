package io.github.gabrielevanger.petja.feature.account

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.github.gabrielevanger.petja.core.domain.DiagnosticLog
import io.github.gabrielevanger.petja.core.domain.account.AccountError
import io.github.gabrielevanger.petja.core.domain.account.AccountState
import io.github.gabrielevanger.petja.core.domain.account.FieldError
import io.github.gabrielevanger.petja.core.domain.account.SignUpResult
import io.github.gabrielevanger.petja.core.domain.storemode.StoreOrderError
import io.github.gabrielevanger.petja.core.domain.testing.ANA
import io.github.gabrielevanger.petja.core.domain.testing.FakeAccountRepository
import io.github.gabrielevanger.petja.core.domain.testing.FakeStoreOrderRepository
import io.github.gabrielevanger.petja.feature.account.forgotpassword.ForgotPasswordAction
import io.github.gabrielevanger.petja.feature.account.forgotpassword.ForgotPasswordEvent
import io.github.gabrielevanger.petja.feature.account.forgotpassword.ForgotPasswordViewModel
import io.github.gabrielevanger.petja.feature.account.form.Field
import io.github.gabrielevanger.petja.feature.account.home.AccountAction
import io.github.gabrielevanger.petja.feature.account.home.AccountEvent
import io.github.gabrielevanger.petja.feature.account.home.AccountViewModel
import io.github.gabrielevanger.petja.feature.account.resetpassword.ResetPasswordAction
import io.github.gabrielevanger.petja.feature.account.resetpassword.ResetPasswordEvent
import io.github.gabrielevanger.petja.feature.account.resetpassword.ResetPasswordViewModel
import io.github.gabrielevanger.petja.feature.account.signin.SignInAction
import io.github.gabrielevanger.petja.feature.account.signin.SignInEvent
import io.github.gabrielevanger.petja.feature.account.signin.SignInViewModel
import io.github.gabrielevanger.petja.feature.account.signup.SignUpAction
import io.github.gabrielevanger.petja.feature.account.signup.SignUpEvent
import io.github.gabrielevanger.petja.feature.account.signup.SignUpViewModel
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

        viewModel.onAction(SignInAction.OnFieldChange(Field.Email, "ana@"))
        assertTrue(viewModel.state.value.fieldErrors.isEmpty(), "sem acusar enquanto digita")

        viewModel.onAction(SignInAction.OnSubmitClick)
        assertEquals(
            mapOf(Field.Email to FieldError.InvalidEmail, Field.Password to FieldError.Required),
            viewModel.state.value.fieldErrors,
        )

        viewModel.onAction(SignInAction.OnFieldChange(Field.Email, "ana@teste.local"))
        assertEquals(mapOf(Field.Password to FieldError.Required), viewModel.state.value.fieldErrors)
        assertFalse(viewModel.state.value.submitting, "não chama o servidor com campo errado")
    }

    @Test
    fun `sign in succeeds`() = runTest(dispatcher) {
        val viewModel = SignInViewModel(accounts, log)
        viewModel.onAction(SignInAction.OnFieldChange(Field.Email, "ana@teste.local"))
        viewModel.onAction(SignInAction.OnFieldChange(Field.Password, "x"))

        viewModel.events.test {
            viewModel.onAction(SignInAction.OnSubmitClick)
            assertTrue(viewModel.state.value.submitting)
            advanceUntilIdle()
            assertEquals(SignInEvent.SignedIn, awaitItem())
        }

        assertEquals(Unit, viewModel.state.value.result)
        assertTrue(accounts.state.value is AccountState.SignedIn)
    }

    @Test
    fun `server error is shown and cleared when typing again`() = runTest(dispatcher) {
        accounts.failWith = AccountError.InvalidCredentials
        val viewModel = SignInViewModel(accounts, log)
        viewModel.onAction(SignInAction.OnFieldChange(Field.Email, "ana@teste.local"))
        viewModel.onAction(SignInAction.OnFieldChange(Field.Password, "errada123"))

        viewModel.onAction(SignInAction.OnSubmitClick)
        advanceUntilIdle()
        assertEquals(AccountError.InvalidCredentials, viewModel.state.value.error)
        assertFalse(viewModel.state.value.submitting)
        assertTrue(warnings.isEmpty(), "erro esperado não vai para o log")

        viewModel.onAction(SignInAction.OnFieldChange(Field.Password, "certa1234"))
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `network errors are logged`() = runTest(dispatcher) {
        accounts.failWith = AccountError.Network
        val viewModel = ForgotPasswordViewModel(accounts, log)
        viewModel.onAction(ForgotPasswordAction.OnFieldChange(Field.Email, "ana@teste.local"))

        viewModel.onAction(ForgotPasswordAction.OnSubmitClick)
        advanceUntilIdle()

        assertEquals(AccountError.Network, viewModel.state.value.error)
        assertEquals(1, warnings.size)
    }

    @Test
    fun `double tap submits once`() = runTest(dispatcher) {
        val viewModel = ForgotPasswordViewModel(accounts, log)
        viewModel.onAction(ForgotPasswordAction.OnFieldChange(Field.Email, "ana@teste.local"))

        viewModel.onAction(ForgotPasswordAction.OnSubmitClick)
        viewModel.onAction(ForgotPasswordAction.OnSubmitClick)
        advanceUntilIdle()

        assertEquals(listOf("ana@teste.local"), accounts.passwordResets)
    }

    @Test
    fun `sign up validates name, email and password`() = runTest(dispatcher) {
        val viewModel = SignUpViewModel(accounts, log)
        viewModel.onAction(SignUpAction.OnFieldChange(Field.Password, "curta"))

        viewModel.onAction(SignUpAction.OnSubmitClick)

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
        viewModel.onAction(SignUpAction.OnFieldChange(Field.Name, "Bia"))
        viewModel.onAction(SignUpAction.OnFieldChange(Field.Email, "bia@teste.local"))
        viewModel.onAction(SignUpAction.OnFieldChange(Field.Password, "segredo123"))

        viewModel.onAction(SignUpAction.OnSubmitClick)
        advanceUntilIdle()

        assertEquals(SignUpResult.ConfirmationEmailSent, viewModel.state.value.result)
        assertEquals(AccountState.SignedOut, accounts.state.value)
    }

    @Test
    fun `reset password needs both fields equal`() = runTest(dispatcher) {
        val viewModel = ResetPasswordViewModel(accounts, log)
        viewModel.onAction(ResetPasswordAction.OnFieldChange(Field.Password, "novasenha123"))
        viewModel.onAction(ResetPasswordAction.OnFieldChange(Field.PasswordConfirmation, "novasenha124"))

        viewModel.onAction(ResetPasswordAction.OnSubmitClick)
        assertEquals(
            mapOf(Field.PasswordConfirmation to FieldError.PasswordsDoNotMatch),
            viewModel.state.value.fieldErrors,
        )

        viewModel.onAction(ResetPasswordAction.OnFieldChange(Field.PasswordConfirmation, "novasenha123"))
        viewModel.onAction(ResetPasswordAction.OnSubmitClick)
        advanceUntilIdle()
        assertEquals("novasenha123", accounts.lastPassword)
    }

    @Test
    fun `account screen state, notice and sign out`() = runTest(dispatcher) {
        val signedIn = FakeAccountRepository(AccountState.SignedIn(ANA))
        val viewModel = AccountViewModel(
            SavedStateHandle(mapOf(NOTICE_ARG to "EmailConfirmed")),
            signedIn,
            FakeStoreOrderRepository(),
            log,
        )
        viewModel.uiState.launchIn(backgroundScope)
        advanceUntilIdle()

        assertEquals(AccountNotice.EmailConfirmed, viewModel.uiState.value.notice)
        assertEquals(AccountState.SignedIn(ANA), viewModel.uiState.value.account)

        viewModel.onAction(AccountAction.OnSignOutClick)
        advanceUntilIdle()
        assertEquals(AccountState.SignedOut, viewModel.uiState.value.account)
    }

    @Test
    fun `sign out failure is only logged`() = runTest(dispatcher) {
        val signedIn = FakeAccountRepository(AccountState.SignedIn(ANA)).apply { failWith = AccountError.Network }
        val viewModel = AccountViewModel(SavedStateHandle(), signedIn, FakeStoreOrderRepository(), log)

        viewModel.onAction(AccountAction.OnSignOutClick)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.notice)
        assertEquals(1, warnings.size)
    }

    @Test
    fun `store mode shortcut only for the store team`() = runTest(dispatcher) {
        suspend fun canOperate(accounts: FakeAccountRepository, stores: FakeStoreOrderRepository): Boolean {
            val viewModel = AccountViewModel(SavedStateHandle(), accounts, stores, log)
            viewModel.uiState.launchIn(backgroundScope)
            advanceUntilIdle()
            return viewModel.uiState.value.canOperateStore
        }

        assertTrue(canOperate(FakeAccountRepository(AccountState.SignedIn(ANA)), FakeStoreOrderRepository()))
        assertFalse(
            canOperate(
                FakeAccountRepository(AccountState.SignedIn(ANA)),
                FakeStoreOrderRepository(stores = emptyList()),
            ),
        )
        assertFalse(
            canOperate(
                FakeAccountRepository(AccountState.SignedIn(ANA)),
                FakeStoreOrderRepository().apply { failWith = StoreOrderError.Network },
            ),
        )
        assertEquals(1, warnings.size, "a falha fica no log e o atalho some")
        assertFalse(canOperate(FakeAccountRepository(), FakeStoreOrderRepository()))
    }

    @Test
    fun `clicks become navigation events`() = runTest(dispatcher) {
        val account = AccountViewModel(SavedStateHandle(), FakeAccountRepository(), FakeStoreOrderRepository(), log)
        account.events.test {
            account.onAction(AccountAction.OnSignInClick)
            assertEquals(AccountEvent.OpenSignIn, awaitItem())
            account.onAction(AccountAction.OnSignUpClick)
            assertEquals(AccountEvent.OpenSignUp, awaitItem())
            account.onAction(AccountAction.OnOrdersClick)
            assertEquals(AccountEvent.OpenOrders, awaitItem())
            account.onAction(AccountAction.OnStoreModeClick)
            assertEquals(AccountEvent.OpenStoreMode, awaitItem())
            account.onAction(AccountAction.OnBackClick)
            assertEquals(AccountEvent.Close, awaitItem())
        }

        val signIn = SignInViewModel(accounts, log)
        signIn.events.test {
            signIn.onAction(SignInAction.OnForgotPasswordClick)
            assertEquals(SignInEvent.OpenForgotPassword, awaitItem())
            signIn.onAction(SignInAction.OnSignUpClick)
            assertEquals(SignInEvent.OpenSignUp, awaitItem())
            signIn.onAction(SignInAction.OnBackClick)
            assertEquals(SignInEvent.Close, awaitItem())
        }
    }

    @Test
    fun `sign up and new password tell the screen when they are done`() = runTest(dispatcher) {
        val signUp = SignUpViewModel(accounts, log)
        signUp.events.test {
            signUp.onAction(SignUpAction.OnFieldChange(Field.Name, "Bia Lima"))
            signUp.onAction(SignUpAction.OnFieldChange(Field.Email, "bia@teste.local"))
            signUp.onAction(SignUpAction.OnFieldChange(Field.Password, "segredo123"))
            signUp.onAction(SignUpAction.OnSubmitClick)
            advanceUntilIdle()
            assertEquals(SignUpEvent.SignedIn, awaitItem())
            signUp.onAction(SignUpAction.OnBackClick)
            assertEquals(SignUpEvent.Close, awaitItem())
        }

        val reset = ResetPasswordViewModel(accounts, log)
        reset.events.test {
            reset.onAction(ResetPasswordAction.OnFieldChange(Field.Password, "novasenha123"))
            reset.onAction(ResetPasswordAction.OnFieldChange(Field.PasswordConfirmation, "novasenha123"))
            reset.onAction(ResetPasswordAction.OnSubmitClick)
            advanceUntilIdle()
            assertEquals(ResetPasswordEvent.Done, awaitItem())
            reset.onAction(ResetPasswordAction.OnBackClick)
            assertEquals(ResetPasswordEvent.Close, awaitItem())
        }

        val forgot = ForgotPasswordViewModel(accounts, log)
        forgot.events.test {
            forgot.onAction(ForgotPasswordAction.OnBackClick)
            assertEquals(ForgotPasswordEvent.Close, awaitItem())
        }
    }
}
