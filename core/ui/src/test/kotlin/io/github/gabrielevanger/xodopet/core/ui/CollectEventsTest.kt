package io.github.gabrielevanger.xodopet.core.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollectEventsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun deliversEachEventOnce() {
        val channel = Channel<String>(Channel.BUFFERED)
        val received = mutableListOf<String>()
        channel.trySend("abrir loja")
        composeRule.setContent { CollectEvents(channel.receiveAsFlow()) { received += it } }

        composeRule.waitForIdle()
        channel.trySend("abrir carrinho")
        composeRule.waitForIdle()

        assertEquals(listOf("abrir loja", "abrir carrinho"), received)
    }
}
