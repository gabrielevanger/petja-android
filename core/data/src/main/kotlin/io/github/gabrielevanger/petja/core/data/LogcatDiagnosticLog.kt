package io.github.gabrielevanger.petja.core.data

import android.util.Log
import io.github.gabrielevanger.petja.core.domain.DiagnosticLog
import javax.inject.Inject

/** Filtre por PetJa no Logcat. */
internal class LogcatDiagnosticLog @Inject constructor() : DiagnosticLog {

    override fun warn(message: String, cause: Throwable?) {
        Log.w(TAG, message, cause)
    }

    private companion object {
        const val TAG = "PetJa"
    }
}
