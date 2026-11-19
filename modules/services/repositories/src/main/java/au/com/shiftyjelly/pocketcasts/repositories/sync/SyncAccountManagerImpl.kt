package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerFuture
import android.accounts.NetworkErrorException
import android.accounts.OnAccountsUpdateListener
import android.content.Intent
import android.os.Bundle
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.preferences.AccountConstants
import au.com.shiftyjelly.pocketcasts.preferences.RefreshToken
import au.com.shiftyjelly.pocketcasts.servers.sync.LoginIdentity
import au.com.shiftyjelly.pocketcasts.servers.sync.TokenHandler
import au.com.shiftyjelly.pocketcasts.servers.sync.exception.RefreshTokenExpiredException
import au.com.shiftyjelly.pocketcasts.utils.extensions.findParcelable
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.eventhorizon.SignInSourceType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

@Singleton
open class SyncAccountManagerImpl @Inject constructor(
    private val tokenErrorNotification: TokenErrorNotification,
    private val accountManager: AccountManager,
) : TokenHandler,
    SyncAccountManager {
    override fun getAccount(): Account? {
        return accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE).firstOrNull()
    }

    override fun isLoggedIn(): Boolean {
        return getAccount() != null
    }

    override fun getEmail(): String? {
        return getAccount()?.name
    }

    override fun emailFlow() = callbackFlow<String?> {
        val listener = object : OnAccountsUpdateListener {
            override fun onAccountsUpdated(accounts: Array<out Account>?) {
                val email = accounts?.find { it.type == AccountConstants.ACCOUNT_TYPE }?.name
                trySend(email)
            }
        }
        trySend(getEmail())
        try {
            accountManager.addOnAccountsUpdatedListener(listener, null, true)
        } catch (e: Exception) {
            // AccountManager stores the listener before the calls that can throw, so it stays registered unless we remove it here
            accountManager.removeOnAccountsUpdatedListener(listener)
            throw e
        }
        awaitClose {
            accountManager.removeOnAccountsUpdatedListener(listener)
        }
    }

    override fun getUuid(): String? = getAccount()?.let { account ->
        accountManager.getUserData(account, AccountConstants.UUID)
    }

    override fun getLoginIdentity(): LoginIdentity? {
        val account = getAccount() ?: return null
        val loginIdentity = accountManager.getUserData(account, AccountConstants.LOGIN_IDENTITY)
        return LoginIdentity.valueOf(loginIdentity) ?: LoginIdentity.PocketCasts
    }

    override fun peekAccessToken(account: Account): AccessToken? = accountManager.peekAuthToken(account, AccountConstants.TOKEN_TYPE)?.let {
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
                    null,
                )
                val bundle: Bundle = resultFuture.result // This call will block until the result is available.
                val token = bundle.getString(AccountManager.KEY_AUTHTOKEN)
                // Token failed to refresh
                if (token == null) {
                    val intent = bundle.findParcelable<Intent>(AccountManager.KEY_INTENT)
                    if (intent == null) {
                        throw NetworkErrorException()
                    } else {
                        tokenErrorNotification.show(intent)
                        throw RefreshTokenExpiredException()
                    }
                } else {
                    AccessToken(token)
                }
            } catch (e: RefreshTokenExpiredException) {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Could not get token because the refresh token has expired")
                throw e
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
        val userData = Bundle().apply {
            putString(AccountConstants.UUID, uuid)
            putString(
                AccountConstants.SIGN_IN_TYPE_KEY,
                AccountConstants.SignInType.Tokens.value,
            )
            putString(AccountConstants.LOGIN_IDENTITY, loginIdentity.key)
            putString(AccountConstants.REFRESH_TOKEN_KEY, refreshToken.value)
        }
        // Adding the account broadcasts LOGIN_ACCOUNTS_CHANGED once anyway, so the password is still set here
        // to keep the account usable by older app versions after a downgrade. It is never rewritten afterwards.
        val accountAdded = accountManager.addAccountExplicitly(account, refreshToken.value, userData)
        accountManager.setAuthToken(account, AccountConstants.TOKEN_TYPE, accessToken.value)

        // When the account was already added, addAccountExplicitly ignores the userData, so set it here
        if (!accountAdded) {
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Account already added, updating the sign in type and refresh token")
            setRefreshToken(refreshToken)
        }
    }

    override fun signOut() {
        accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE).forEach { account ->
            accountManager.removeAccountExplicitly(account)
        }
    }

    /**
     * Don't use AccountManager.setPassword here. It runs on every access token refresh, and each call deletes the
     * account's auth tokens and broadcasts LOGIN_ACCOUNTS_CHANGED, which cold starts every app on the device that
     * listens for it. setUserData doesn't broadcast.
     */
    override fun setRefreshToken(refreshToken: RefreshToken) {
        val account = getAccount() ?: return
        setUserDataIfChanged(account, AccountConstants.REFRESH_TOKEN_KEY, refreshToken.value)
        setUserDataIfChanged(account, AccountConstants.SIGN_IN_TYPE_KEY, AccountConstants.SignInType.Tokens.value)
    }

    private fun setUserDataIfChanged(account: Account, key: String, value: String) {
        if (accountManager.getUserData(account, key) != value) {
            accountManager.setUserData(account, key, value)
        }
    }

    override fun setAccessToken(accessToken: AccessToken) {
        val account = getAccount() ?: return
        accountManager.setAuthToken(account, AccountConstants.TOKEN_TYPE, accessToken.value)
    }

    override fun getRefreshToken(account: Account?): RefreshToken? {
        val currentAccount = account ?: getAccount() ?: return null
        val refreshToken = accountManager.getUserData(currentAccount, AccountConstants.REFRESH_TOKEN_KEY)
        if (!refreshToken.isNullOrEmpty()) {
            return RefreshToken(refreshToken)
        }
        // Accounts signed in before the refresh token moved to user data only have it as the password, so copy it across once.
        // The legacy password is left in place because clearing it would broadcast LOGIN_ACCOUNTS_CHANGED.
        val legacyRefreshToken = accountManager.getPassword(currentAccount)
        if (legacyRefreshToken.isNullOrEmpty()) {
            return null
        }
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Migrating the refresh token from the account password to user data")
        accountManager.setUserData(currentAccount, AccountConstants.REFRESH_TOKEN_KEY, legacyRefreshToken)
        return RefreshToken(legacyRefreshToken)
    }

    override fun setEmail(email: String) {
        val account = getAccount() ?: return
        accountManager.renameAccount(account, email, null, null)
    }
}

sealed class SignInSource {
    sealed class UserInitiated(val analyticsValue: SignInSourceType) : SignInSource() {
        object SignInViewModel : UserInitiated(SignInSourceType.SignInViewModel)
        object Onboarding : UserInitiated(SignInSourceType.Onboarding)
        object Watch : UserInitiated(SignInSourceType.Watch)
    }
    object WatchPhoneSync : SignInSource()
}
