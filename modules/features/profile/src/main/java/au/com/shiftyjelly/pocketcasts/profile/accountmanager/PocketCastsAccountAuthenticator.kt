package au.com.shiftyjelly.pocketcasts.profile.accountmanager

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import au.com.shiftyjelly.pocketcasts.account.AccountActivity
import au.com.shiftyjelly.pocketcasts.preferences.AccountConstants
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import kotlinx.coroutines.runBlocking

class PocketCastsAccountAuthenticator(
    val context: Context,
    private val syncManager: SyncManager,
) : AbstractAccountAuthenticator(context) {
    override fun getAuthTokenLabel(authTokenType: String?): String {
        return AccountConstants.TOKEN_TYPE
    }

    override fun confirmCredentials(response: AccountAuthenticatorResponse?, account: Account?, options: Bundle?): Bundle {
        return Bundle()
    }

    override fun updateCredentials(response: AccountAuthenticatorResponse?, account: Account?, authTokenType: String?, options: Bundle?): Bundle {
        return Bundle()
    }

    override fun getAuthToken(response: AccountAuthenticatorResponse?, account: Account?, authTokenType: String?, options: Bundle?): Bundle {
        if (account == null) {
            return buildSignInIntent(response)
        }

        val accessToken = runBlocking { syncManager.getAccessToken(account) }

        return if (accessToken == null) {
            buildSignInIntent(response)
        } else {
            bundleOf(
                AccountManager.KEY_ACCOUNT_NAME to account.name,
                AccountManager.KEY_ACCOUNT_TYPE to account.type,
                AccountManager.KEY_AUTHTOKEN to accessToken.value
            )
        }
    }

    private fun buildSignInIntent(response: AccountAuthenticatorResponse?): Bundle {
        // Could not get auth token so display sign in sign up screens
        val intent = Intent(context, AccountActivity::class.java).apply {
            putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        }
        return bundleOf(AccountManager.KEY_INTENT to intent)
    }

    override fun hasFeatures(response: AccountAuthenticatorResponse?, account: Account?, features: Array<out String>?): Bundle {
        return Bundle()
    }

    override fun editProperties(response: AccountAuthenticatorResponse?, accountType: String?): Bundle {
        return Bundle()
    }

    override fun addAccount(response: AccountAuthenticatorResponse?, accountType: String?, authTokenType: String?, requiredFeatures: Array<out String>?, options: Bundle?): Bundle {
        val intent = Intent(context, AccountActivity::class.java).apply {
            putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        }
        return bundleOf(AccountManager.KEY_INTENT to intent)
    }
}
