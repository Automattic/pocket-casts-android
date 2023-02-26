package au.com.shiftyjelly.pocketcasts.servers.model
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken

data class AuthResultModel(
    val token: AccessToken,
    val uuid: String,
    val isNewAccount: Boolean
)
