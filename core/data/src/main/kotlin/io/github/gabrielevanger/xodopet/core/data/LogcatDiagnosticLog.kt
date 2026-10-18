package io.github.gabrielevanger.xodopet.core.data

import android.util.Log
import io.github.gabrielevanger.xodopet.core.domain.DiagnosticLog
import javax.inject.Inject

/** Filtre por XodoPet no Logcat. */
internal class LogcatDiagnosticLog @Inject constructor() : DiagnosticLog {

    override fun warn(message: String, cause: Throwable?) {
        Log.w(TAG, message, cause)
    }

    private companion object {
        const val TAG = "XodoPet"
    }
}
