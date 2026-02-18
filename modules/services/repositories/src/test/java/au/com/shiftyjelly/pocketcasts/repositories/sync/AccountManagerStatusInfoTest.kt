package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import androidx.core.content.edit
import au.com.shiftyjelly.pocketcasts.preferences.AccountConstants
import au.com.shiftyjelly.pocketcasts.utils.extensions.getString
import java.util.UUID
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class AccountManagerStatusInfoTest {
    private val accountManager = mock<AccountManager>()
    private val sharedPrefs = RuntimeEnvironment.getApplication().getSharedPreferences("prefs", Context.MODE_PRIVATE)
    private val accountManagerStatusInfo = AccountManagerStatusInfo(accountManager, sharedPrefs)

    @Before
    fun setup() {
        sharedPrefs.edit().clear()
    }

    @Test
    fun `test isLoggedIn returns true when account exists`() {
        val account = Account("test@example.com", AccountConstants.ACCOUNT_TYPE)

        whenever(accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE)).thenReturn(arrayOf(account))

        assertTrue(accountManagerStatusInfo.isLoggedIn())
    }

    @Test
    fun `test isLoggedIn returns false when no account exists`() {
        whenever(accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE)).thenReturn(emptyArray())

        assertFalse(accountManagerStatusInfo.isLoggedIn())
    }

    @Test
    fun `test getUserIds returns account ID when account exists`() {
        val account = Account("test@example.com", AccountConstants.ACCOUNT_TYPE)
        whenever(accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE)).thenReturn(arrayOf(account))
        whenever(accountManager.getUserData(account, AccountConstants.UUID)).thenReturn("1234-5678-uuid")

        val userIds = accountManagerStatusInfo.getUserIds()

        assertEquals("1234-5678-uuid", userIds.accountId)
    }

    @Test
    fun `test getUserIds returns null account ID when no account exists`() {
        whenever(accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE)).thenReturn(emptyArray())

        val userIds = accountManagerStatusInfo.getUserIds()

        assertNull(userIds.accountId)
    }

    @Test
    fun `test getUserIds returns anon ID from shared prefs`() {
        whenever(accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE)).thenReturn(emptyArray())
        val anonId = UUID.randomUUID().toString()
        sharedPrefs.edit {
            putString(AccountManagerStatusInfo.ANON_ID_KEY, anonId)
        }

        val userIds = accountManagerStatusInfo.getUserIds()

        assertEquals(anonId, userIds.anonId)
    }

    @Test
    fun `test getUserIds saves anon ID to shared prefs`() {
        whenever(accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE)).thenReturn(emptyArray())
        val userIds = accountManagerStatusInfo.getUserIds()

        assertEquals(userIds.anonId, sharedPrefs.getString(AccountManagerStatusInfo.ANON_ID_KEY))
    }
}
