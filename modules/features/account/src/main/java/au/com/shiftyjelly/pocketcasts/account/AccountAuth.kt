package au.com.shiftyjelly.pocketcasts.account

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsPropValue
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.TracksAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.localization.helper.LocaliseHelper
import au.com.shiftyjelly.pocketcasts.preferences.AccountConstants
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsThread
import au.com.shiftyjelly.pocketcasts.servers.ServerCallback
import au.com.shiftyjelly.pocketcasts.servers.ServerManager
import au.com.shiftyjelly.pocketcasts.servers.model.AuthResultModel
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
        private const val KEY_SIGN_IN_SOURCE = "sign_in_source"
        private const val KEY_ERROR_CODE = "error_code"
    }

    suspend fun signInWithEmailAndPassword(
        email: String,
        password: String,
        signInSource: SignInSource
    ): AuthResult {
        return withContext(Dispatchers.IO) {
            val authResult = loginToSyncServer(email, password)
            if (authResult is AuthResult.Success) {
                signInSuccessful(email, password, authResult.result)
            }
            trackSignIn(authResult, signInSource)
            authResult
        }
    }

    private fun trackSignIn(authResult: AuthResult, signInSource: SignInSource) {
        val properties = mapOf(KEY_SIGN_IN_SOURCE to signInSource.analyticsValue)
        when (authResult) {
            is AuthResult.Success -> {
                analyticsTracker.track(AnalyticsEvent.USER_SIGNED_IN, properties)
            }
            is AuthResult.Failed -> {
                val errorCodeValue = authResult.serverMessageId ?: TracksAnalyticsTracker.INVALID_OR_NULL_VALUE
                val errorProperties = properties.plus(KEY_ERROR_CODE to AnalyticsPropValue(errorCodeValue))
                analyticsTracker.track(AnalyticsEvent.USER_SIGNIN_FAILED, errorProperties)
            }
        }
    }

    suspend fun createUserWithEmailAndPassword(email: String, password: String): AuthResult {
        return withContext(Dispatchers.IO) {
            val authResult = registerWithSyncServer(email, password)
            if (authResult is AuthResult.Success) {
                signInSuccessful(email, password, authResult.result)
            }
            authResult
        }
    }

    private suspend fun registerWithSyncServer(email: String, password: String): AuthResult {
        return suspendCoroutine { continuation ->
            serverManager.registerWithSyncServer(
                email, password,
                object : ServerCallback<AuthResultModel> {
                    override fun dataReturned(result: AuthResultModel?) {
                        continuation.resume(AuthResult.Success(result))
                        analyticsTracker.track(AnalyticsEvent.USER_ACCOUNT_CREATED)
                    }

                    override fun onFailed(
                        errorCode: Int,
                        userMessage: String?,
                        serverMessageId: String?,
                        serverMessage: String?,
                        throwable: Throwable?
                    ) {
                        val message = LocaliseHelper.serverMessageIdToMessage(serverMessageId, ::getResourceString)
                            ?: userMessage
                            ?: getResourceString(LR.string.error_login_failed)
                        continuation.resume(
                            AuthResult.Failed(
                                message = message,
                                serverMessageId = serverMessageId
                            )
                        )
                        val errorCodeValue = serverMessageId ?: TracksAnalyticsTracker.INVALID_OR_NULL_VALUE
                        val properties = mapOf(KEY_ERROR_CODE to AnalyticsPropValue(errorCodeValue))
                        analyticsTracker.track(AnalyticsEvent.USER_ACCOUNT_CREATION_FAILED, properties)
                    }
                }
            )
        }
    }

    private suspend fun loginToSyncServer(email: String, password: String): AuthResult {
        return suspendCoroutine { continuation ->
            serverManager.loginToSyncServer(
                email, password,
                object : ServerCallback<AuthResultModel> {
                    override fun dataReturned(result: AuthResultModel?) {
                        continuation.resume(AuthResult.Success(result))
                    }

                    override fun onFailed(
                        errorCode: Int,
                        userMessage: String?,
                        serverMessageId: String?,
                        serverMessage: String?,
                        throwable: Throwable?
                    ) {
                        val message = LocaliseHelper.serverMessageIdToMessage(serverMessageId, ::getResourceString)
                            ?: userMessage
                            ?: getResourceString(LR.string.error_login_failed)
                        continuation.resume(
                            AuthResult.Failed(
                                message = message,
                                serverMessageId = serverMessageId
                            )
                        )
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
                    complete(AuthResult.Success(null))
                    analyticsTracker.track(AnalyticsEvent.USER_PASSWORD_RESET)
                }

                override fun onFailed(
                    errorCode: Int,
                    userMessage: String?,
                    serverMessageId: String?,
                    serverMessage: String?,
                    throwable: Throwable?
                ) {
                    val message = LocaliseHelper.serverMessageIdToMessage(serverMessageId, ::getResourceString)
                        ?: userMessage
                        ?: getResourceString(LR.string.profile_reset_password_failed)
                    complete(
                        AuthResult.Failed(
                            message = message,
                            serverMessageId = serverMessageId
                        )
                    )
                }
            }
        )
    }

    private suspend fun signInSuccessful(email: String, password: String, authResult: AuthResultModel?) {
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Signed in successfully to $email")
        // Store details in android account manager
        if (authResult?.token != null && authResult.token?.isNotEmpty() == true) {
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Saving $email to account manager")
            val account = Account(email, AccountConstants.ACCOUNT_TYPE)
            val accountManager = AccountManager.get(context)
            accountManager.addAccountExplicitly(account, password, null)
            accountManager.setAuthToken(account, AccountConstants.TOKEN_TYPE, authResult.token)
            accountManager.setUserData(account, AccountConstants.UUID, authResult.uuid)

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

    sealed class AuthResult {
        data class Success(val result: AuthResultModel?) : AuthResult()
        data class Failed(val message: String, val serverMessageId: String?) : AuthResult()
    }
}

enum class SignInSource(analyticsString: String) {
    AccountAuthenticator("account_manager"),
    SignInViewModel("sign_in_view_model"),
    PocketCastsApplication("pocketcasts_application");

    val analyticsValue = AnalyticsPropValue(analyticsString)
}
