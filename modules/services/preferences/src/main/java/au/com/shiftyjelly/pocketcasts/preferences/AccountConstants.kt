package au.com.shiftyjelly.pocketcasts.preferences

import android.accounts.Account
import android.accounts.AccountManager

object AccountConstants {
    val ACCOUNT_TYPE = (if (BuildConfig.DEBUG) "au.com.shiftyjelly.pocketcasts.debug" else "au.com.shiftyjelly.pocketcasts") + ".pocketcasts"
    const val TOKEN_TYPE = "sync"
    const val UUID = "uuid"
    const val SIGN_IN_TYPE_KEY = "sign_in"

    sealed class SignInType(val value: String) {
        object EmailPassword : SignInType("EmailPassword")
        object RefreshToken : SignInType("RefreshToken")

        override fun toString() = value

        companion object {
            fun fromString(value: String?): SignInType {
                return if (value != null && value == RefreshToken.value) RefreshToken else EmailPassword
            }
        }
    }
}

fun AccountManager.pocketCastsAccount(): Account? {
    return getAccountsByType(AccountConstants.ACCOUNT_TYPE).firstOrNull()
}

fun AccountManager.getSignInType(account: Account): AccountConstants.SignInType {
    return AccountConstants.SignInType.fromString(getUserData(account, AccountConstants.SIGN_IN_TYPE_KEY))
}
