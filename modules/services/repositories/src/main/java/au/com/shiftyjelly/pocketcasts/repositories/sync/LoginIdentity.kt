package au.com.shiftyjelly.pocketcasts.repositories.sync

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

sealed class LoginIdentity(val value: String) {
    object PocketCasts : LoginIdentity("PocketCasts")
    object Google : LoginIdentity("Google")

    companion object {
        fun valueOf(value: String?): LoginIdentity? {
            return when (value) {
                PocketCasts.value -> PocketCasts
                Google.value -> Google
                else -> null
            }
        }
    }

    object Adapter {
        @ToJson
        fun toJson(loginIdentity: LoginIdentity): String = loginIdentity.value

        @FromJson
        fun fromJson(value: String): LoginIdentity? = valueOf(value)
    }
}
