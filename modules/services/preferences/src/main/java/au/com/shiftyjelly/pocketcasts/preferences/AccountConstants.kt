package au.com.shiftyjelly.pocketcasts.preferences

import android.accounts.Account
import android.accounts.AccountManager
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

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

fun AccountManager.setRefreshToken(account: Account, refreshToken: RefreshToken) {
    setPassword(account, refreshToken.value)
}

fun AccountManager.setAccessToken(account: Account, authTokenType: String, accessToken: AccessToken) {
    setAuthToken(account, authTokenType, accessToken.value)
}

fun AccountManager.peekAccessToken(account: Account, authTokenType: String): AccessToken? =
    peekAuthToken(account, authTokenType)?.let {
        if (it.isNotEmpty()) {
            AccessToken(it)
        } else {
            null
        }
    }

fun AccountManager.invalidateAccessToken() {
    val account = pocketCastsAccount() ?: return
    peekAccessToken(account, AccountConstants.TOKEN_TYPE)?.let { token ->
        invalidateAuthToken(AccountConstants.ACCOUNT_TYPE, token.value)
    }
}

@JvmInline
value class AccessToken(val value: String) {
    object Adapter : JsonAdapter<AccessToken>() {
        override fun fromJson(reader: JsonReader) = AccessToken(reader.nextString())

        override fun toJson(writer: JsonWriter, accessToken: AccessToken?) {
            writer.value(accessToken?.value)
        }
    }
}

@JvmInline
value class RefreshToken(val value: String) {
    object Adapter : JsonAdapter<RefreshToken>() {
        override fun fromJson(reader: JsonReader) = RefreshToken((reader.nextString()))

        override fun toJson(writer: JsonWriter, refreshToken: RefreshToken?) {
            writer.value(refreshToken?.value)
        }
    }
}
