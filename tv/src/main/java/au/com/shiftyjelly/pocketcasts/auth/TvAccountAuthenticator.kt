package au.com.shiftyjelly.pocketcasts.auth

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.content.Context
import android.os.Bundle

class TvAccountAuthenticator(context: Context) : AbstractAccountAuthenticator(context) {
    override fun editProperties(response: AccountAuthenticatorResponse?, accountType: String?) = Bundle()
    override fun addAccount(response: AccountAuthenticatorResponse?, accountType: String?, authTokenType: String?, requiredFeatures: Array<out String>?, options: Bundle?) = Bundle()
    override fun confirmCredentials(response: AccountAuthenticatorResponse?, account: Account?, options: Bundle?) = Bundle()
    override fun getAuthToken(response: AccountAuthenticatorResponse?, account: Account?, authTokenType: String?, options: Bundle?) = Bundle()
    override fun getAuthTokenLabel(authTokenType: String?) = ""
    override fun updateCredentials(response: AccountAuthenticatorResponse?, account: Account?, authTokenType: String?, options: Bundle?) = Bundle()
    override fun hasFeatures(response: AccountAuthenticatorResponse?, account: Account?, features: Array<out String>?) = Bundle()
}
