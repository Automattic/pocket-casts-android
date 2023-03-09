package au.com.shiftyjelly.pocketcasts.servers.account

import au.com.shiftyjelly.pocketcasts.servers.model.AuthResultModel

sealed class LoginResult {
    data class Success(val result: AuthResultModel) : LoginResult()
    data class Failed(val message: String, val messageId: String?) : LoginResult()
}
