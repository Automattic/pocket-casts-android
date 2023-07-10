package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerFuture
import android.accounts.NetworkErrorException
import android.content.Intent
import android.os.Bundle
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.preferences.AccountConstants
import au.com.shiftyjelly.pocketcasts.preferences.RefreshToken
import au.com.shiftyjelly.pocketcasts.servers.sync.TokenHandler
import au.com.shiftyjelly.pocketcasts.servers.sync.exception.RefreshTokenExpiredException
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class SyncAccountManagerImpl @Inject constructor(
    private val tokenErrorNotification: TokenErrorNotification,
    private val accountManager: AccountManager
) : TokenHandler, SyncAccountManager {

    override fun getAccount(): Account? {
        return accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE).firstOrNull()
    }

    override fun isLoggedIn(): Boolean {
        return getAccount() != null
    }

    override fun getEmail(): String? {
        return getAccount()?.name
    }

    override fun getUuid(): String? =
        getAccount()?.let { account ->
            accountManager.getUserData(account, AccountConstants.UUID)
        }

    override fun getLoginIdentity(): LoginIdentity? {
        val account = getAccount() ?: return null
        val loginIdentity = accountManager.getUserData(account, AccountConstants.LOGIN_IDENTITY)
        return LoginIdentity.valueOf(loginIdentity) ?: LoginIdentity.PocketCasts
    }

    override fun peekAccessToken(account: Account): AccessToken? =
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
                    val intent = BundleCompat.getParcelable(bundle, AccountManager.KEY_INTENT, Intent::class.java)
                    if (intent == null) {
                        throw NetworkErrorException()
                    } else {
                        tokenErrorNotification.show(intent)
                        throw RefreshTokenExpiredException()
                    }
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

    override fun getSignInType(account: Account): AccountConstants.SignInType {
        return AccountConstants.SignInType.fromString(accountManager.getUserData(account, AccountConstants.SIGN_IN_TYPE_KEY))
    }

    override fun addAccount(email: String, uuid: String, refreshToken: RefreshToken, accessToken: AccessToken, loginIdentity: LoginIdentity) {
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

    override fun signOut() {
        accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE).forEach { account ->
            accountManager.removeAccountExplicitly(account)
        }
    }

    override fun setRefreshToken(refreshToken: RefreshToken) {
        val account = getAccount() ?: return
        accountManager.setPassword(account, refreshToken.value)
        accountManager.setUserData(account, AccountConstants.SIGN_IN_TYPE_KEY, AccountConstants.SignInType.Tokens.value)
    }

    override fun setAccessToken(accessToken: AccessToken) {
        val account = getAccount() ?: return
        accountManager.setAuthToken(account, AccountConstants.TOKEN_TYPE, accessToken.value)
    }

    override fun getRefreshToken(account: Account?): RefreshToken? =
        (account ?: getAccount())?.let {
            val refreshToken = accountManager.getPassword(it)
            if (refreshToken != null && refreshToken.isNotEmpty()) {
                RefreshToken(refreshToken)
            } else {
                null
            }
        }

    override fun setEmail(email: String) {
        val account = getAccount() ?: return
        accountManager.renameAccount(account, email, null, null)
    }
}

sealed class SignInSource {
    sealed class UserInitiated(val analyticsValue: String) : SignInSource() {
        object SignInViewModel : UserInitiated("sign_in_view_model")
        object Onboarding : UserInitiated("onboarding")
        object Watch : UserInitiated("watch")
    }
    object WatchPhoneSync : SignInSource()
}
