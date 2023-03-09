package au.com.shiftyjelly.pocketcasts.servers.account

sealed class LoginIdentity(val value: String) {
    object PocketCasts : LoginIdentity("PocketCasts")
    object Google : LoginIdentity("Google")

    companion object {
        fun valueOf(value: String?): LoginIdentity {
            return when (value) {
                PocketCasts.value -> PocketCasts
                Google.value -> Google
                else -> PocketCasts
            }
        }
    }
}
