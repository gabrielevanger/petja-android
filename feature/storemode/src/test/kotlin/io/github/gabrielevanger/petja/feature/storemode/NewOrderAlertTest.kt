package io.github.gabrielevanger.petja.feature.storemode

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NewOrderAlertTest {

    @Test
    fun playsWithoutCrashingEvenWithoutARingtone() {
        // No Robolectric não há som de notificação configurado: o alerta só não pode derrubar o app.
        RingtoneNewOrderAlert(ApplicationProvider.getApplicationContext()).play()
    }
}
