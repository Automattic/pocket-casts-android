package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.OnAccountsUpdateListener
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.preferences.AccountConstants
import au.com.shiftyjelly.pocketcasts.preferences.RefreshToken
import au.com.shiftyjelly.pocketcasts.servers.sync.LoginIdentity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
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

    @Test
    fun `setRefreshToken stores the token in user data and never sets the password`() {
        val account = stubAccounts("test@example.com").single()

        syncAccountManager.setRefreshToken(RefreshToken("new_refresh_token"))

        verify(accountManager).setUserData(account, AccountConstants.REFRESH_TOKEN_KEY, "new_refresh_token")
        verify(accountManager).setUserData(account, AccountConstants.SIGN_IN_TYPE_KEY, AccountConstants.SignInType.Tokens.value)
        verify(accountManager, never()).setPassword(any(), anyOrNull())
    }

    @Test
    fun `setRefreshToken skips the write when nothing changed`() {
        val account = stubAccounts("test@example.com").single()
        whenever(accountManager.getUserData(account, AccountConstants.REFRESH_TOKEN_KEY)).thenReturn("same_refresh_token")
        whenever(accountManager.getUserData(account, AccountConstants.SIGN_IN_TYPE_KEY)).thenReturn(AccountConstants.SignInType.Tokens.value)

        syncAccountManager.setRefreshToken(RefreshToken("same_refresh_token"))

        verify(accountManager, never()).setUserData(any(), any(), anyOrNull())
        verify(accountManager, never()).setPassword(any(), anyOrNull())
    }

    @Test
    fun `refresh token round trips through user data`() {
        val account = stubAccounts("test@example.com").single()
        var stored: String? = null
        whenever(accountManager.setUserData(eq(account), eq(AccountConstants.REFRESH_TOKEN_KEY), anyOrNull())).thenAnswer {
            stored = it.arguments[2] as String?
            null
        }
        whenever(accountManager.getUserData(account, AccountConstants.REFRESH_TOKEN_KEY)).thenAnswer { stored }

        syncAccountManager.setRefreshToken(RefreshToken("rotated_refresh_token"))

        assertEquals(RefreshToken("rotated_refresh_token"), syncAccountManager.getRefreshToken())
        verify(accountManager, never()).getPassword(any())
    }

    @Test
    fun `getRefreshToken migrates a legacy password once`() {
        val account = stubAccounts("test@example.com").single()
        var stored: String? = null
        whenever(accountManager.getPassword(account)).thenReturn("legacy_refresh_token")
        whenever(accountManager.setUserData(eq(account), eq(AccountConstants.REFRESH_TOKEN_KEY), anyOrNull())).thenAnswer {
            stored = it.arguments[2] as String?
            null
        }
        whenever(accountManager.getUserData(account, AccountConstants.REFRESH_TOKEN_KEY)).thenAnswer { stored }

        assertEquals(RefreshToken("legacy_refresh_token"), syncAccountManager.getRefreshToken())
        assertEquals(RefreshToken("legacy_refresh_token"), syncAccountManager.getRefreshToken())

        verify(accountManager, times(1)).setUserData(account, AccountConstants.REFRESH_TOKEN_KEY, "legacy_refresh_token")
        verify(accountManager, times(1)).getPassword(account)
        verify(accountManager, never()).setPassword(any(), anyOrNull())
    }

    @Test
    fun `getRefreshToken returns null without any stored token`() {
        stubAccounts("test@example.com")

        assertNull(syncAccountManager.getRefreshToken())
        verify(accountManager, never()).setUserData(any(), any(), anyOrNull())
    }

    @Test
    fun `addAccount stores the refresh token in user data`() {
        whenever(accountManager.addAccountExplicitly(any(), anyOrNull(), anyOrNull())).thenReturn(true)

        syncAccountManager.addAccount(
            email = "test@example.com",
            uuid = "uuid",
            refreshToken = RefreshToken("sign_in_refresh_token"),
            accessToken = AccessToken("access_token"),
            loginIdentity = LoginIdentity.PocketCasts,
        )

        verify(accountManager).addAccountExplicitly(
            eq(Account("test@example.com", AccountConstants.ACCOUNT_TYPE)),
            eq("sign_in_refresh_token"),
            argThat { getString(AccountConstants.REFRESH_TOKEN_KEY) == "sign_in_refresh_token" },
        )
        verify(accountManager, never()).setPassword(any(), anyOrNull())
    }

    @Test
    fun `addAccount updates the refresh token when the account already exists`() {
        val account = stubAccounts("test@example.com").single()
        whenever(accountManager.addAccountExplicitly(any(), anyOrNull(), anyOrNull())).thenReturn(false)

        syncAccountManager.addAccount(
            email = "test@example.com",
            uuid = "uuid",
            refreshToken = RefreshToken("new_sign_in_refresh_token"),
            accessToken = AccessToken("access_token"),
            loginIdentity = LoginIdentity.PocketCasts,
        )

        verify(accountManager).setUserData(account, AccountConstants.REFRESH_TOKEN_KEY, "new_sign_in_refresh_token")
        verify(accountManager).setUserData(account, AccountConstants.SIGN_IN_TYPE_KEY, AccountConstants.SignInType.Tokens.value)
        verify(accountManager, never()).setPassword(any(), anyOrNull())
    }

    private fun stubAccounts(vararg emails: String): Array<Account> {
        val accounts = emails.map { Account(it, AccountConstants.ACCOUNT_TYPE) }.toTypedArray()
        whenever(accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE)).thenReturn(accounts)
        return accounts
    }
}
