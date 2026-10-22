package io.github.gabrielevanger.petja.core.domain.account

import java.io.IOException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class AccountModelsTest {
    @Test
    fun `exception keeps the reason and the original cause`() {
        val cause = IOException("sem rede")

        val exception = AccountException(AccountError.Network, cause)

        assertEquals(AccountError.Network, exception.error)
        assertEquals("Network", exception.message)
        assertSame(cause, exception.cause)
    }

    @Test
    fun `every reason has a stable name for logs`() {
        assertEquals(
            listOf(
                "InvalidCredentials", "EmailNotConfirmed", "EmailAlreadyInUse", "WeakPassword", "SamePassword",
                "InvalidEmail", "TooManyEmails", "Network", "Unknown",
            ),
            AccountError.entries.map { it.name },
        )
    }

    @Test
    fun `states and results are distinct`() {
        val signedIn = AccountState.SignedIn(Account("ana", "ana@teste.local", null))

        assertNotEquals(AccountState.Loading as AccountState, AccountState.SignedOut as AccountState)
        assertEquals("ana", signedIn.account.id)
        assertEquals(2, SignUpResult.entries.size)
        assertEquals(4, AuthLinkResult.entries.size)
    }
}
