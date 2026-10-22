package io.github.gabrielevanger.petja.core.domain.checkout

import io.github.gabrielevanger.petja.core.domain.account.FieldError

/** Rua, número e bairro são obrigatórios; complemento e referência ajudam o entregador, mas são opcionais. */
object AddressValidation {
    fun required(value: String): FieldError? = if (value.isBlank()) FieldError.Required else null
}
