package au.com.shiftyjelly.pocketcasts.servers.account

import au.com.shiftyjelly.pocketcasts.servers.model.AuthResultModel

sealed class SignInResult {
    data class Success(val result: AuthResultModel) : SignInResult()
    data class Failed(val message: String, val messageId: String?) : SignInResult()
}
