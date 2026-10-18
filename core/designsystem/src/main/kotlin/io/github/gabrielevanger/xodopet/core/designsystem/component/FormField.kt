package io.github.gabrielevanger.xodopet.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import io.github.gabrielevanger.xodopet.core.designsystem.theme.XodoTheme

enum class FormFieldType { Text, Name, Email, Password }

/**
 * Campo de formulário com rótulo e erro embaixo. O tipo escolhe o teclado, a capitalização e,
 * para senha, esconde o texto e deixa o gerenciador de senhas do aparelho preencher.
 */
@Composable
fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    type: FormFieldType = FormFieldType.Text,
    error: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        enabled = enabled,
        singleLine = true,
        shape = XodoTheme.shapes.card,
        visualTransformation = if (type ==
            FormFieldType.Password
        ) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = when (type) {
                FormFieldType.Email -> KeyboardType.Email
                FormFieldType.Password -> KeyboardType.Password
                FormFieldType.Text, FormFieldType.Name -> KeyboardType.Text
            },
            capitalization = if (type ==
                FormFieldType.Name
            ) {
                KeyboardCapitalization.Words
            } else {
                KeyboardCapitalization.None
            },
            autoCorrectEnabled = type == FormFieldType.Text,
            imeAction = imeAction,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
