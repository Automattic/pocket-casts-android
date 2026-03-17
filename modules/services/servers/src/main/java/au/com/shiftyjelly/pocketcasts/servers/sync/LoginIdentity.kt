package au.com.shiftyjelly.pocketcasts.servers.sync

import com.automattic.eventhorizon.LoginIdentityType
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

sealed class LoginIdentity(
    val key: String,
    val analyticsValue: LoginIdentityType,
) {
    object PocketCasts : LoginIdentity(
        key = "PocketCasts",
        analyticsValue = LoginIdentityType.Password,
    )

    object Google : LoginIdentity(
        key = "Google",
        analyticsValue = LoginIdentityType.Google,
    )

    companion object {
        fun valueOf(value: String?): LoginIdentity? {
            return when (value) {
                PocketCasts.key -> PocketCasts
                Google.key -> Google
                else -> null
            }
        }
    }

    object Adapter {
        @ToJson
        fun toJson(loginIdentity: LoginIdentity): String = loginIdentity.key

        @FromJson
        fun fromJson(value: String): LoginIdentity? = valueOf(value)
    }
}
