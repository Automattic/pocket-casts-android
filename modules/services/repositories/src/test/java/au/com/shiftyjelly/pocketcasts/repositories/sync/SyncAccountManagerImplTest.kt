package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.OnAccountsUpdateListener
import au.com.shiftyjelly.pocketcasts.preferences.AccountConstants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class SyncAccountManagerImplTest {
    private val accountManager = mock<AccountManager>()
    private val syncAccountManager = SyncAccountManagerImpl(mock(), accountManager)

    @Test
    fun `emailFlow emits the current email`() = runTest {
        stubAccounts("test@example.com")

        assertEquals("test@example.com", syncAccountManager.emailFlow().first())
    }

    @Test
    fun `emailFlow emits every account update`() = runTest {
        stubAccounts("first@example.com")
        whenever(accountManager.addOnAccountsUpdatedListener(any(), anyOrNull(), any())).thenAnswer { invocation ->
            val listener = invocation.arguments[0] as OnAccountsUpdateListener
            listener.onAccountsUpdated(arrayOf(Account("second@example.com", AccountConstants.ACCOUNT_TYPE)))
            null
        }

        val emails = syncAccountManager.emailFlow().take(2).toList()

        assertEquals(listOf("first@example.com", "second@example.com"), emails)
    }

    @Test
    fun `emailFlow deregisters the listener when it is cancelled`() = runTest {
        stubAccounts()

        syncAccountManager.emailFlow().first()

        verify(accountManager).removeOnAccountsUpdatedListener(any())
    }

    @Test
    fun `emailFlow deregisters the listener when registering it throws`() = runTest {
        stubAccounts()
        whenever(accountManager.addOnAccountsUpdatedListener(any(), anyOrNull(), any()))
            .thenThrow(IllegalStateException("registration failed"))

        val error = runCatching { syncAccountManager.emailFlow().toList() }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        verify(accountManager).removeOnAccountsUpdatedListener(any())
    }

    @Test
    fun `emailFlow does not deregister a listener it never registered`() = runTest {
        whenever(accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE)).thenThrow(SecurityException("no access"))

        val error = runCatching { syncAccountManager.emailFlow().toList() }.exceptionOrNull()

        assertTrue(error is SecurityException)
        verify(accountManager, never()).removeOnAccountsUpdatedListener(any())
    }

    private fun stubAccounts(vararg emails: String) {
        val accounts = emails.map { Account(it, AccountConstants.ACCOUNT_TYPE) }.toTypedArray()
        whenever(accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE)).thenReturn(accounts)
    }
}
