package au.com.shiftyjelly.pocketcasts.preferences

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

object AccountConstants {
    val ACCOUNT_TYPE = (if (BuildConfig.DEBUG) "au.com.shiftyjelly.pocketcasts.debug" else "au.com.shiftyjelly.pocketcasts") + ".pocketcasts"
    const val TOKEN_TYPE = "sync"
    const val UUID = "uuid"
    const val SIGN_IN_TYPE_KEY = "sign_in"
    const val LOGIN_IDENTITY = "login_identity"

    sealed class SignInType(val value: String) {
        object Password : SignInType("Password")
        object Tokens : SignInType("Tokens")

        override fun toString() = value

        companion object {
            fun fromString(value: String?): SignInType {
                return if (value != null && value == Tokens.value) Tokens else Password
            }
        }
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
        override fun fromJson(reader: JsonReader) = RefreshToken(reader.nextString())

        override fun toJson(writer: JsonWriter, refreshToken: RefreshToken?) {
            writer.value(refreshToken?.value)
        }
    }
}
