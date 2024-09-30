package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.accounts.Account
import android.accounts.AccountManager
import au.com.shiftyjelly.pocketcasts.analytics.AccountStatusInfo
import au.com.shiftyjelly.pocketcasts.preferences.AccountConstants
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class AccountManagerStatusInfoTest {

    private lateinit var accountManager: AccountManager
    private lateinit var accountManagerStatusInfo: AccountStatusInfo

    @Before
    fun setUp() {
        accountManager = mock(AccountManager::class.java)
        accountManagerStatusInfo = AccountManagerStatusInfo(accountManager)
    }

    @Test
    fun `test isLoggedIn returns true when account exists`() {
        val account = Account("test@example.com", AccountConstants.ACCOUNT_TYPE)

        `when`(accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE)).thenReturn(arrayOf(account))

        assertTrue(accountManagerStatusInfo.isLoggedIn())
    }

    @Test
    fun `test isLoggedIn returns false when no account exists`() {
        `when`(accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE)).thenReturn(emptyArray())

        assertFalse(accountManagerStatusInfo.isLoggedIn())
    }

    @Test
    fun `test getUuid returns UUID when account exists`() {
        val account = Account("test@example.com", AccountConstants.ACCOUNT_TYPE)
        `when`(accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE)).thenReturn(arrayOf(account))
        `when`(accountManager.getUserData(account, AccountConstants.UUID)).thenReturn("1234-5678-uuid")

        val uuid = accountManagerStatusInfo.getUuid()

        assertEquals("1234-5678-uuid", uuid)
    }

    @Test
    fun `test getUuid returns null when no account exists`() {
        `when`(accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE)).thenReturn(emptyArray())

        val uuid = accountManagerStatusInfo.getUuid()

        assertNull(uuid)
    }
}
