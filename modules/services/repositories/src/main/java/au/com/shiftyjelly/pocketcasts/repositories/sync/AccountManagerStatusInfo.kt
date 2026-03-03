package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.accounts.AccountManager
import android.content.SharedPreferences
import androidx.core.content.edit
import au.com.shiftyjelly.pocketcasts.analytics.AccountStatusInfo
import au.com.shiftyjelly.pocketcasts.analytics.UserIds
import au.com.shiftyjelly.pocketcasts.preferences.AccountConstants
import au.com.shiftyjelly.pocketcasts.preferences.di.PublicSharedPreferences
import au.com.shiftyjelly.pocketcasts.utils.extensions.getString
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountManagerStatusInfo @Inject constructor(
    private val accountManager: AccountManager,
    @PublicSharedPreferences private val sharedPreferences: SharedPreferences,
) : AccountStatusInfo {
    override fun isLoggedIn() = getAccount() != null

    @Synchronized
    override fun getUserIds() = UserIds(
        accountId = getAccountId(),
        anonId = getOrCreateAnonId(),
    )

    @Synchronized
    override fun recreateAnonId(): String {
        sharedPreferences.edit {
            remove(ANON_ID_KEY)
        }
        return getOrCreateAnonId()
    }

    private fun getAccountId() = getAccount()?.let { account ->
        accountManager.getUserData(account, AccountConstants.UUID)
    }

    private fun getAccount() = accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE).firstOrNull()

    private fun getOrCreateAnonId(): String {
        val id = sharedPreferences.getString(ANON_ID_KEY)
        return if (id == null) {
            val uuid = UUID.randomUUID().toString().replace("-", "")
            sharedPreferences.edit {
                putString(ANON_ID_KEY, uuid)
            }
            uuid
        } else {
            id
        }
    }

    internal companion object {
        // Legacy key value
        const val ANON_ID_KEY = "nosara_tracks_anon_id"
    }
}
