package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.accounts.AccountManager
import au.com.shiftyjelly.pocketcasts.analytics.AccountStatusInfo
import au.com.shiftyjelly.pocketcasts.preferences.AccountConstants
import javax.inject.Inject

class AccountManagerStatusInfo @Inject constructor(
    private val accountManager: AccountManager,
) : AccountStatusInfo {
    fun getAccount() = accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE).firstOrNull()

    override fun isLoggedIn() = getAccount() != null

    override fun getUuid() = getAccount()?.let { account ->
        accountManager.getUserData(account, AccountConstants.UUID)
    }
}
