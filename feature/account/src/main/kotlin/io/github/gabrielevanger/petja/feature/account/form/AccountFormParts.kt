package io.github.gabrielevanger.petja.feature.account.form

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import io.github.gabrielevanger.petja.core.designsystem.component.FormField
import io.github.gabrielevanger.petja.core.designsystem.component.FormFieldType
import io.github.gabrielevanger.petja.core.designsystem.component.PetjaButton
import io.github.gabrielevanger.petja.core.designsystem.theme.PetjaTheme
import io.github.gabrielevanger.petja.core.ui.BackTopBar
import io.github.gabrielevanger.petja.feature.account.R
import io.github.gabrielevanger.petja.feature.account.message

// Peças que as telas de conta repetem: moldura com voltar, campos de e-mail e senha, erro e botão de enviar.

@Composable
internal fun AccountScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(containerColor = PetjaTheme.colors.background, topBar = {
        BackTopBar(title, onBack = onBack)
    }) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(PetjaTheme.spacing.m),
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PetjaTheme.spacing.l, vertical = PetjaTheme.spacing.m),
            content = content,
        )
    }
}

@Composable
internal fun EmailField(
    state: FormUiState<*>,
    onChange: (Field, String) -> Unit,
    imeAction: ImeAction = ImeAction.Next,
) {
    FormField(
        value = state.value(Field.Email),
        onValueChange = { onChange(Field.Email, it) },
        label = stringResource(R.string.field_email),
        type = FormFieldType.Email,
        error = state.fieldErrors[Field.Email].message(),
        enabled = !state.submitting,
        imeAction = imeAction,
    )
}

@Composable
internal fun PasswordField(
    state: FormUiState<*>,
    onChange: (Field, String) -> Unit,
    field: Field,
    @StringRes label: Int,
    imeAction: ImeAction,
) {
    FormField(
        value = state.value(field),
        onValueChange = { onChange(field, it) },
        label = stringResource(label),
        type = FormFieldType.Password,
        error = state.fieldErrors[field].message(),
        enabled = !state.submitting,
        imeAction = imeAction,
    )
}

@Composable
internal fun FormError(state: FormUiState<*>) {
    state.error?.let {
        Text(text = it.message(), style = PetjaTheme.typography.body, color = PetjaTheme.colors.error)
    }
}

@Composable
internal fun SubmitButton(@StringRes label: Int, submitting: Boolean, onSubmit: () -> Unit) {
    PetjaButton(
        text = stringResource(label),
        onClick = onSubmit,
        loading = submitting,
        modifier = Modifier.fillMaxWidth(),
    )
}
