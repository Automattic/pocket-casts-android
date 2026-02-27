package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import com.automattic.eventhorizon.LoginIdentity as EventHorizonLoginIdentity

sealed class LoginIdentity(
    val key: String,
    val eventHorizonValue: EventHorizonLoginIdentity,
) {
    object PocketCasts : LoginIdentity(
        key = "PocketCasts",
        eventHorizonValue = EventHorizonLoginIdentity.Password,
    )

    object Google : LoginIdentity(
        key = "Google",
        eventHorizonValue = EventHorizonLoginIdentity.Google,
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
