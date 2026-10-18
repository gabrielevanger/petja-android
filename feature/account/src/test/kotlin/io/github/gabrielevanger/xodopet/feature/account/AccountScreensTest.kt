package io.github.gabrielevanger.xodopet.feature.account

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme
import io.github.gabrielevanger.xodopet.core.domain.account.AccountError
import io.github.gabrielevanger.xodopet.core.domain.account.AccountState
import io.github.gabrielevanger.xodopet.core.domain.account.FieldError
import io.github.gabrielevanger.xodopet.core.domain.account.SignUpResult
import io.github.gabrielevanger.xodopet.core.domain.testing.ANA
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(content: @Composable () -> Unit) = composeRule.setContent { XodoTheme { content() } }

    @Test
    fun signedOutOffersSignInAndSignUp() {
        var signIn = 0
        var signUp = 0
        show { AccountScreen(AccountState.SignedOut, null, {}, { signIn++ }, { signUp++ }, {}) }

        composeRule.onNodeWithText("Entrar").performClick()
        composeRule.onNodeWithText("Criar conta").performClick()
        assertEquals(1, signIn)
        assertEquals(1, signUp)
    }

    @Test
    fun signedInShowsNameAndSignsOut() {
        var signOut = 0
        var orders = 0
        show {
            AccountScreen(AccountState.SignedIn(ANA), AccountNotice.EmailConfirmed, {
            }, {}, {}, { signOut++ }, { orders++ })
        }

        composeRule.onNodeWithText("E-mail confirmado. Sua conta está pronta.").assertExists()
        composeRule.onNodeWithText("Olá, Ana Souza").assertExists()
        composeRule.onNodeWithText("Meus pedidos").performClick()
        composeRule.onNodeWithText("Sair da conta").performClick()
        assertEquals(1, signOut)
        assertEquals(1, orders)
    }

    @Test
    fun accountWithoutNameAndOtherNotices() {
        show {
            AccountScreen(AccountState.SignedIn(ANA.copy(fullName = null)), AccountNotice.LinkExpired, {}, {}, {}, {})
        }

        composeRule.onNodeWithText("Olá!").assertExists()
        composeRule.onNodeWithText("Esse link venceu", substring = true).assertExists()
    }

    @Test
    fun loadingAccountAndPasswordChangedNotice() {
        show { AccountScreen(AccountState.Loading, AccountNotice.PasswordChanged, {}, {}, {}, {}) }

        composeRule.onNodeWithText("Senha alterada.").assertExists()
        composeRule.onNodeWithText("Carregando sua conta").assertExists()
    }

    @Test
    fun signInShowsFieldAndServerErrors() {
        var typed: Pair<Field, String>? = null
        var forgot = 0
        val state = FormUiState<Unit>(
            fieldErrors = mapOf(Field.Email to FieldError.InvalidEmail, Field.Password to FieldError.Required),
            error = AccountError.InvalidCredentials,
        )
        show { SignInScreen(state, {}, { field, value -> typed = field to value }, {}, { forgot++ }, {}) }

        composeRule.onNodeWithText("Confira o e-mail.").assertExists()
        composeRule.onNodeWithText("Preencha este campo.").assertExists()
        composeRule.onNodeWithText("E-mail ou senha incorretos.").assertExists()
        composeRule.onNodeWithText("E-mail").performTextInput("ana@teste.local")
        assertEquals(Field.Email to "ana@teste.local", typed)
        composeRule.onNodeWithText("Esqueci minha senha").performClick()
        assertEquals(1, forgot)
    }

    @Test
    fun submittingDisablesTheButton() {
        show { SignInScreen(FormUiState(submitting = true), {}, { _, _ -> }, {}, {}, {}) }

        composeRule.onNode(hasText("Entrar") and hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun everyServerErrorHasItsOwnMessage() {
        val messages = mutableListOf<String>()
        show {
            Column {
                AccountError.entries.forEach { error ->
                    val text = error.message()
                    messages += text
                    Text(text)
                }
            }
        }

        assertEquals(AccountError.entries.size, messages.distinct().size)
    }

    @Test
    fun signUpFormAndConfirmation() {
        var submitted = 0
        show {
            SignUpScreen(
                FormUiState(
                    fieldErrors = mapOf(
                        Field.Name to FieldError.NameTooShort,
                        Field.Password to FieldError.PasswordTooShort,
                    ),
                ),
                {},
                { _, _ -> },
                { submitted++ },
            )
        }

        composeRule.onNodeWithText("Escreva seu nome.").assertExists()
        composeRule.onNodeWithText("Use pelo menos 8 caracteres.").assertExists()
        composeRule.onNode(hasText("Criar conta") and hasClickAction()).performClick()
        assertEquals(1, submitted)
    }

    @Test
    fun signUpWaitingForEmail() {
        show {
            SignUpScreen(
                FormUiState(
                    values = mapOf(Field.Email to "bia@teste.local "),
                    result = SignUpResult.ConfirmationEmailSent,
                ),
                {},
                { _, _ -> },
                {},
            )
        }

        composeRule.onNodeWithText("Enviamos um link para bia@teste.local", substring = true).assertExists()
    }

    @Test
    fun forgotPasswordBeforeAndAfterSending() {
        show {
            ForgotPasswordScreen(FormUiState(values = mapOf(Field.Email to "ana@teste.local"), result = Unit), {
            }, { _, _ -> }, {})
        }

        composeRule.onNodeWithText("Se houver uma conta com ana@teste.local", substring = true).assertExists()
    }

    @Test
    fun forgotPasswordForm() {
        var submitted = 0
        show { ForgotPasswordScreen(FormUiState(), {}, { _, _ -> }, { submitted++ }) }

        composeRule.onNodeWithText("Enviar link").performClick()
        assertEquals(1, submitted)
    }

    @Test
    fun resetPasswordForm() {
        var submitted = 0
        show {
            ResetPasswordScreen(
                FormUiState(fieldErrors = mapOf(Field.PasswordConfirmation to FieldError.PasswordsDoNotMatch)),
                {},
                { _, _ -> },
                { submitted++ },
            )
        }

        composeRule.onNodeWithText("As senhas não são iguais.").assertExists()
        composeRule.onNodeWithText("Salvar senha").performClick()
        assertEquals(1, submitted)
    }
}
