package io.github.gabrielevanger.xodopet.feature.account

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.gabrielevanger.xodopet.core.domain.DiagnosticLog
import io.github.gabrielevanger.xodopet.core.domain.account.AccountError
import io.github.gabrielevanger.xodopet.core.domain.account.AccountException
import io.github.gabrielevanger.xodopet.core.domain.account.FieldError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Campos de texto dos formulários de conta. */
enum class Field { Name, Email, Password, PasswordConfirmation }

/**
 * Estado comum dos formulários de conta.
 *
 * @property fieldErrors aparecem só depois da primeira tentativa de enviar, para não acusar erro enquanto a pessoa digita.
 * @property result o que a tela mostra ou faz depois de dar certo; null enquanto não terminou.
 */
@Immutable
data class FormUiState<R>(
    val values: Map<Field, String> = emptyMap(),
    val fieldErrors: Map<Field, FieldError> = emptyMap(),
    val submitting: Boolean = false,
    val error: AccountError? = null,
    val result: R? = null,
) {
    fun value(field: Field) = values[field].orEmpty()
}

/**
 * Base dos formulários: guarda os campos, valida antes de enviar e traduz falhas em [AccountError].
 * Cada formulário diz quais regras conferir e o que fazer ao enviar.
 */
abstract class AccountFormViewModel<R>(private val log: DiagnosticLog) : ViewModel() {
    private val _state = MutableStateFlow(FormUiState<R>())
    val state: StateFlow<FormUiState<R>> = _state.asStateFlow()

    private var submitted = false

    protected abstract fun validate(values: Map<Field, String>): Map<Field, FieldError>

    protected abstract suspend fun send(values: Map<Field, String>): R

    fun onChange(field: Field, value: String) = _state.update {
        val values = it.values + (field to value)
        it.copy(values = values, fieldErrors = if (submitted) validate(values) else emptyMap(), error = null)
    }

    fun submit() {
        if (_state.value.submitting) return
        submitted = true
        val values = _state.value.values
        val errors = validate(values)
        _state.update { it.copy(fieldErrors = errors, error = null) }
        if (errors.isNotEmpty()) return
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            try {
                val result = send(values)
                _state.update { it.copy(submitting = false, result = result) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: AccountException) {
                if (e.error == AccountError.Unknown || e.error == AccountError.Network) log.warn("Falha na conta", e)
                _state.update { it.copy(submitting = false, error = e.error) }
            }
        }
    }
}

internal fun Map<Field, FieldError?>.withoutValid(): Map<Field, FieldError> =
    entries.mapNotNull { (field, error) -> error?.let { field to it } }.toMap()
