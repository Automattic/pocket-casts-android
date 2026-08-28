package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.accounts.NetworkErrorException
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import au.com.shiftyjelly.pocketcasts.preferences.AccountConstants
import au.com.shiftyjelly.pocketcasts.servers.sync.exception.RefreshTokenExpiredException
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import kotlinx.coroutines.runBlocking

/**
 * Refreshes the access token for the Pocket Casts account. Each app supplies the sign in screen for its form factor.
 */
class PocketCastsAccountAuthenticator(
    private val context: Context,
    private val syncManager: SyncManager,
    private val signInActivity: Class<out Activity>,
) : AbstractAccountAuthenticator(context) {

    override fun getAuthToken(response: AccountAuthenticatorResponse?, account: Account?, authTokenType: String?, options: Bundle?): Bundle {
        if (account == null) {
            return buildSignInIntent(response)
        }
        return try {
            val accessToken = runBlocking { syncManager.getAccessToken(account) }
            Bundle().apply {
                putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
                putString(AccountManager.KEY_ACCOUNT_TYPE, account.type)
                putString(AccountManager.KEY_AUTHTOKEN, accessToken.value)
            }
        } catch (e: RefreshTokenExpiredException) {
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Refresh token expired, sign in required")
            buildSignInIntent(response)
        } catch (e: Exception) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "Failed to refresh the access token")
            throw NetworkErrorException(e)
        }
    }

    override fun addAccount(
        response: AccountAuthenticatorResponse?,
        accountType: String?,
        authTokenType: String?,
        requiredFeatures: Array<out String>?,
        options: Bundle?,
    ) = buildSignInIntent(response)

    // A bundle with neither an auth token nor an intent makes callers throw, so every path that can't produce a token returns a sign in intent.
    private fun buildSignInIntent(response: AccountAuthenticatorResponse?): Bundle {
        val intent = Intent(context, signInActivity).apply {
            putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        }
        return Bundle().apply {
            putParcelable(AccountManager.KEY_INTENT, intent)
        }
    }

    override fun getAuthTokenLabel(authTokenType: String?) = AccountConstants.TOKEN_TYPE
    override fun editProperties(response: AccountAuthenticatorResponse?, accountType: String?) = Bundle()
    override fun confirmCredentials(response: AccountAuthenticatorResponse?, account: Account?, options: Bundle?) = Bundle()
    override fun updateCredentials(response: AccountAuthenticatorResponse?, account: Account?, authTokenType: String?, options: Bundle?) = Bundle()
    override fun hasFeatures(response: AccountAuthenticatorResponse?, account: Account?, features: Array<out String>?) = Bundle().apply {
        // The account advertises no features, so only an empty request can be satisfied.
        putBoolean(AccountManager.KEY_BOOLEAN_RESULT, features.isNullOrEmpty())
    }
}
