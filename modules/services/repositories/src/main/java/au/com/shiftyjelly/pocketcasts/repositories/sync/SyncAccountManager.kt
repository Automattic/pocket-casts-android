package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerFuture
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.os.bundleOf
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.preferences.AccountConstants
import au.com.shiftyjelly.pocketcasts.preferences.RefreshToken
import au.com.shiftyjelly.pocketcasts.servers.sync.TokenHandler
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * The only class that should use this class is the
 * [SyncManager] class. Consider using that instead of this class.
 */
class SyncAccountManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenErrorNotification: TokenErrorNotification,
) : TokenHandler {

    private val accountManager = AccountManager.get(context)

    private fun getAccount(): Account? {
        return accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE).firstOrNull()
    }

    fun isLoggedIn(): Boolean {
        return getAccount() != null
    }

    fun getEmail(): String? {
        return getAccount()?.name
    }

    fun getUuid(): String? =
        getAccount()?.let { account ->
            accountManager.getUserData(account, AccountConstants.UUID)
        }

    fun getLoginIdentity(): LoginIdentity? {
        val account = getAccount() ?: return null
        val loginIdentity = accountManager.getUserData(account, AccountConstants.LOGIN_IDENTITY)
        return LoginIdentity.valueOf(loginIdentity) ?: LoginIdentity.PocketCasts
    }

    fun peekAccessToken(account: Account): AccessToken? =
        accountManager.peekAuthToken(account, AccountConstants.TOKEN_TYPE)?.let {
            if (it.isNotEmpty()) {
                AccessToken(it)
            } else {
                null
            }
        }

    override suspend fun getAccessToken(): AccessToken? {
        val account = getAccount() ?: return null
        return withContext(Dispatchers.IO) {
            try {
                val resultFuture: AccountManagerFuture<Bundle> = accountManager.getAuthToken(
                    account,
                    AccountConstants.TOKEN_TYPE,
                    Bundle(),
                    false,
                    null,
                    null
                )
                val bundle: Bundle = resultFuture.result // This call will block until the result is available.
                val token = bundle.getString(AccountManager.KEY_AUTHTOKEN)
                // Token failed to refresh
                if (token == null) {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        bundle.getParcelable(AccountManager.KEY_INTENT, Intent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        bundle.getParcelable(AccountManager.KEY_INTENT) as? Intent
                    }
                    intent?.let { tokenErrorNotification.show(it) }
                    throw SecurityException("Token could not be refreshed")
                } else {
                    AccessToken(token)
                }
            } catch (e: Exception) {
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "Could not get token")
                throw e // Rethrow the exception so it carries on
            }
        }
    }

    override fun invalidateAccessToken() {
        val account = getAccount() ?: return
        val accessToken = peekAccessToken(account) ?: return
        accountManager.invalidateAuthToken(AccountConstants.ACCOUNT_TYPE, accessToken.value)
    }

    fun getSignInType(account: Account): AccountConstants.SignInType {
        return AccountConstants.SignInType.fromString(accountManager.getUserData(account, AccountConstants.SIGN_IN_TYPE_KEY))
    }

    fun addAccount(email: String, uuid: String, refreshToken: RefreshToken, accessToken: AccessToken, loginIdentity: LoginIdentity) {
        val account = Account(email, AccountConstants.ACCOUNT_TYPE)
        val userData = bundleOf(
            AccountConstants.UUID to uuid,
            AccountConstants.SIGN_IN_TYPE_KEY to AccountConstants.SignInType.Tokens.value,
            AccountConstants.LOGIN_IDENTITY to loginIdentity.value
        )
        val accountAdded = accountManager.addAccountExplicitly(account, refreshToken.value, userData)
        accountManager.setAuthToken(account, AccountConstants.TOKEN_TYPE, accessToken.value)

        // When the account was already added, set the sign in type to Tokens because the account
        // does not seem to get updated with this from the userData in the addAccountExplicitly call
        if (!accountAdded) {
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Account already added, setting sign in type to Tokens")
            accountManager.setUserData(account, AccountConstants.SIGN_IN_TYPE_KEY, AccountConstants.SignInType.Tokens.value)
        }
    }

    fun signOut() {
        accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE).forEach { account ->
            accountManager.removeAccountExplicitly(account)
        }
    }

    fun setRefreshToken(refreshToken: RefreshToken) {
        val account = getAccount() ?: return
        accountManager.setPassword(account, refreshToken.value)
        accountManager.setUserData(account, AccountConstants.SIGN_IN_TYPE_KEY, AccountConstants.SignInType.Tokens.value)
    }

    fun setAccessToken(accessToken: AccessToken) {
        val account = getAccount() ?: return
        accountManager.setAuthToken(account, AccountConstants.TOKEN_TYPE, accessToken.value)
    }

    fun getRefreshToken(account: Account? = null): RefreshToken? =
        (account ?: getAccount())?.let {
            val refreshToken = accountManager.getPassword(it)
            if (refreshToken.isNotEmpty()) {
                RefreshToken(refreshToken)
            } else {
                null
            }
        }

    fun setEmail(email: String) {
        val account = getAccount() ?: return
        accountManager.renameAccount(account, email, null, null)
    }
}

enum class SignInSource(val analyticsValue: String) {
    AccountAuthenticator("account_manager"),
    SignInViewModel("sign_in_view_model"),
    Onboarding("onboarding"),
    WatchPhoneSync("watch_phone_sync"),
}
