package io.github.gabrielevanger.petja.core.domain

/** Registro de situações anormais que não devem interromper o app, como uma falha de rede já tratada na tela. */
fun interface DiagnosticLog {
    fun warn(message: String, cause: Throwable?)
}
