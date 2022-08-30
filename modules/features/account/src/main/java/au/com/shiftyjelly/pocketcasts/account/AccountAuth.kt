package au.com.shiftyjelly.pocketcasts.account

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.preferences.AccountConstants
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsThread
import au.com.shiftyjelly.pocketcasts.servers.ServerCallback
import au.com.shiftyjelly.pocketcasts.servers.ServerManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Singleton
class AccountAuth @Inject constructor(
    private val settings: Settings,
    private val serverManager: ServerManager,
    private val podcastManager: PodcastManager,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    @ApplicationContext private val context: Context
) {

    companion object {
        const val KEY_SIGN_IN_SOURCE = "sign_in_source"
    }

    suspend fun signInWithEmailAndPassword(
        email: String,
        password: String,
        signInSource: SignInSource
    ): AuthResult {
        return withContext(Dispatchers.IO) {
            val authResult = loginToSyncServer(email, password)
            if (authResult is AuthResult.Success) {
                val token = authResult.result
                signInSuccessful(email, password, token)
                analyticsTracker.track(
                    AnalyticsEvent.USER_SIGNED_IN,
                    mapOf(KEY_SIGN_IN_SOURCE to signInSource.analyticsValue)
                )
            }
            authResult
        }
    }

    suspend fun createUserWithEmailAndPassword(email: String, password: String): AuthResult {
        return withContext(Dispatchers.IO) {
            val authResult = registerWithSyncServer(email, password)
            if (authResult is AuthResult.Success) {
                val token = authResult.result
                signInSuccessful(email, password, token)
            }
            authResult
        }
    }

    private suspend fun registerWithSyncServer(email: String, password: String): AuthResult {
        return suspendCoroutine { continuation ->
            serverManager.registerWithSyncServer(
                email, password,
                object : ServerCallback<String> {
                    override fun dataReturned(result: String?) {
                        continuation.resume(AuthResult.Success(result))
                    }

                    override fun callFailed(errorCode: Int, userMessage: String?, userMessageId: Int?, serverMessage: String?, throwable: Throwable?) {
                        val message = userMessageId?.let { getResourceString(userMessageId) } ?: userMessage ?: getResourceString(LR.string.error_login_failed)
                        continuation.resume(AuthResult.Failed(message = message, field = InputField.GENERAL))
                    }
                }
            )
        }
    }

    private suspend fun loginToSyncServer(email: String, password: String): AuthResult {
        return suspendCoroutine { continuation ->
            serverManager.loginToSyncServer(
                email, password,
                object : ServerCallback<String> {
                    override fun dataReturned(result: String?) {
                        continuation.resume(AuthResult.Success(result))
                    }

                    override fun callFailed(errorCode: Int, userMessage: String?, userMessageId: Int?, serverMessage: String?, throwable: Throwable?) {
                        val message = userMessageId?.let { getResourceString(userMessageId) } ?: userMessage ?: getResourceString(LR.string.error_login_failed)
                        continuation.resume(AuthResult.Failed(message = message, field = InputField.GENERAL))
                    }
                }
            )
        }
    }

    fun resetPasswordWithEmail(email: String, complete: (AuthResult) -> Unit) {
        serverManager.forgottenPasswordToSyncServer(
            email,
            object : ServerCallback<String> {
                override fun dataReturned(result: String?) {
                    complete(AuthResult.Success(result))
                }

                override fun callFailed(errorCode: Int, userMessage: String?, userMessageId: Int?, serverMessage: String?, throwable: Throwable?) {
                    val message = userMessageId?.let { getResourceString(userMessageId) } ?: userMessage ?: getResourceString(LR.string.profile_reset_password_failed)
                    complete(AuthResult.Failed(message, InputField.GENERAL))
                }
            }
        )
    }

    private suspend fun signInSuccessful(email: String, password: String, token: String?) {
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Signed in successfully to $email")
        // Store details in android account manager
        if (token != null && token.isNotEmpty()) {
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Saving $email to account manager")
            val account = Account(email, AccountConstants.ACCOUNT_TYPE)
            val accountManager = AccountManager.get(context)
            accountManager.addAccountExplicitly(account, password, null)
            accountManager.setAuthToken(account, AccountConstants.TOKEN_TYPE, token)

            settings.setUsedAccountManager(true)
        } else {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Sign in marked as successful but we didn't get a token back.")
        }

        settings.setLastModified(null)
        RefreshPodcastsThread.clearLastRefreshTime()
        podcastManager.markAllPodcastsUnsynced()
        podcastManager.refreshPodcasts("login")
    }

    private fun getResourceString(stringId: Int): String {
        return context.resources.getString(stringId)
    }

    enum class InputField {
        EMAIL,
        PASSWORD,
        GENERAL
    }

    sealed class AuthResult {
        data class Success(val result: String?) : AuthResult()
        data class Failed(val message: String, val field: InputField) : AuthResult() {
            val isPasswordError: Boolean = field == InputField.PASSWORD
        }

        val isSuccess: Boolean
            get() = this is Success
    }
}

enum class SignInSource(val analyticsValue: String) {
    AccountAuthenticator("account_manager"),
    SignInViewModel("sign_in_view_model"),
    AutomotiveApplication("automotive_application")
}
