package io.github.gabrielevanger.petja.feature.storemode

import android.content.Context
import android.media.RingtoneManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Avisa a loja que chegou pedido (RF-81). Interface para os testes não tocarem som de verdade. */
fun interface NewOrderAlert {
    fun play()
}

/** Toca o som de notificação escolhido no aparelho, o mesmo que a loja já reconhece. */
internal class RingtoneNewOrderAlert @Inject constructor(@ApplicationContext private val context: Context) :
    NewOrderAlert {
    override fun play() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        RingtoneManager.getRingtone(context, uri)?.play()
    }
}
