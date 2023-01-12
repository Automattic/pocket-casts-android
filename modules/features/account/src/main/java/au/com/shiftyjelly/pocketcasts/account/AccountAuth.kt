package au.com.shiftyjelly.pocketcasts.account

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.TracksAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.AccountConstants
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsThread
import au.com.shiftyjelly.pocketcasts.servers.ServerManager
import au.com.shiftyjelly.pocketcasts.servers.model.AuthResultModel
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import au.com.shiftyjelly.pocketcasts.servers.sync.parseErrorResponse
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.qualifiers.ApplicationContext
import retrofit2.HttpException
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Singleton
class AccountAuth @Inject constructor(
    private val settings: Settings,
    private val serverManager: ServerManager,
    private val syncServerManager: SyncServerManager,
    private val podcastManager: PodcastManager,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val KEY_SIGN_IN_SOURCE = "sign_in_source"
        private const val KEY_ERROR_CODE = "error_code"
    }

    suspend fun signInWithGoogle(
        idToken: String,
        signInSource: SignInSource,
    ): AuthResult {
        val authResult = try {
            val response = syncServerManager.loginGoogle(idToken)
            val result = AuthResultModel(token = response.refreshToken, uuid = response.uuid, isNewAccount = response.isNew)
            signInSuccessful(
                email = response.email,
                refreshTokenOrPassword = response.refreshToken,
                accessToken = response.accessToken,
                userUuid = response.uuid,
                signInType = AccountConstants.SignInType.RefreshToken
            )
            AuthResult.Success(result)
        } catch (ex: Exception) {
            Timber.e(ex, "Failed to sign in with Google")
            exceptionToAuthResult(exception = ex, fallbackMessage = LR.string.error_login_failed)
        }
        trackSignIn(authResult, signInSource)
        return authResult
    }

    suspend fun signInWithEmailAndPassword(
        email: String,
        password: String,
        signInSource: SignInSource
    ): AuthResult {
        val authResult = login(email, password)
        if (authResult is AuthResult.Success) {
            val result = authResult.result
            signInSuccessful(
                email = email,
                refreshTokenOrPassword = password,
                accessToken = result.token,
                userUuid = result.uuid,
                signInType = AccountConstants.SignInType.EmailPassword
            )
        }
        trackSignIn(authResult, signInSource)
        return authResult
    }

    private fun trackSignIn(authResult: AuthResult, signInSource: SignInSource) {
        val properties = mapOf(KEY_SIGN_IN_SOURCE to signInSource.analyticsValue)
        when (authResult) {
            is AuthResult.Success -> {
                analyticsTracker.track(AnalyticsEvent.USER_SIGNED_IN, properties)
            }
            is AuthResult.Failed -> {
                val errorCodeValue = authResult.messageId ?: TracksAnalyticsTracker.INVALID_OR_NULL_VALUE
                val errorProperties = properties.plus(KEY_ERROR_CODE to errorCodeValue)
                analyticsTracker.track(AnalyticsEvent.USER_SIGNIN_FAILED, errorProperties)
            }
        }
    }

    suspend fun createUserWithEmailAndPassword(email: String, password: String): AuthResult {
        val authResult = register(email, password)
        if (authResult is AuthResult.Success) {
            val result = authResult.result
            signInSuccessful(
                email = email,
                refreshTokenOrPassword = password,
                accessToken = result.token,
                userUuid = result.uuid,
                signInType = AccountConstants.SignInType.EmailPassword
            )
        }
        trackRegister(authResult)
        return authResult
    }

    private fun trackRegister(authResult: AuthResult) {
        when (authResult) {
            is AuthResult.Success -> {
                analyticsTracker.track(AnalyticsEvent.USER_ACCOUNT_CREATED)
            }
            is AuthResult.Failed -> {
                val errorCodeValue = authResult.messageId ?: TracksAnalyticsTracker.INVALID_OR_NULL_VALUE
                analyticsTracker.track(AnalyticsEvent.USER_ACCOUNT_CREATION_FAILED, mapOf(KEY_ERROR_CODE to errorCodeValue))
            }
        }
    }

    private suspend fun register(email: String, password: String): AuthResult {
        return try {
            val response = syncServerManager.register(email = email, password = password)
            AuthResult.Success(AuthResultModel(token = response.token, uuid = response.uuid, isNewAccount = true))
        } catch (ex: Exception) {
            exceptionToAuthResult(exception = ex, fallbackMessage = LR.string.error_login_failed)
        }
    }

    private suspend fun login(email: String, password: String): AuthResult {
        return try {
            val response = syncServerManager.login(email = email, password = password)
            val result = AuthResultModel(token = response.token, uuid = response.uuid, isNewAccount = false)
            AuthResult.Success(result)
        } catch (ex: Exception) {
            exceptionToAuthResult(exception = ex, fallbackMessage = LR.string.error_login_failed)
        }
    }

    private fun exceptionToAuthResult(exception: Exception, fallbackMessage: Int): AuthResult.Failed {
        val resources = context.resources
        var message: String? = null
        var messageId: String? = null
        if (exception is HttpException) {
            val errorResponse = exception.parseErrorResponse()
            message = errorResponse?.messageLocalized(resources)
            messageId = errorResponse?.messageId
        }
        message = message ?: resources.getString(fallbackMessage)
        return AuthResult.Failed(message = message, messageId = messageId)
    }

    suspend fun forgotPassword(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            val response = syncServerManager.forgotPassword(email = email)
            if (response.success) {
                analyticsTracker.track(AnalyticsEvent.USER_PASSWORD_RESET)
                onSuccess()
            } else {
                onError(response.message)
            }
        } catch (ex: Exception) {
            Timber.e(ex, "Failed to reset password.")
            onError(getResourceString(LR.string.profile_reset_password_failed))
        }
    }

    private suspend fun signInSuccessful(email: String, refreshTokenOrPassword: String, accessToken: String, userUuid: String, signInType: AccountConstants.SignInType) {
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Signed in successfully to $email")
        // Store details in android account manager
        val account = Account(email, AccountConstants.ACCOUNT_TYPE)
        val accountManager = AccountManager.get(context)
        accountManager.addAccountExplicitly(account, refreshTokenOrPassword, null)
        accountManager.setAuthToken(account, AccountConstants.TOKEN_TYPE, accessToken)
        accountManager.setUserData(account, AccountConstants.UUID, userUuid)
        accountManager.setUserData(account, AccountConstants.SIGN_IN_TYPE_KEY, signInType.toString())

        settings.setUsedAccountManager(true)
        startPodcastRefresh()
    }

    private suspend fun startPodcastRefresh() {
        settings.setLastModified(null)
        RefreshPodcastsThread.clearLastRefreshTime()
        podcastManager.markAllPodcastsUnsynced()
        podcastManager.refreshPodcasts("login")
    }

    suspend fun refreshToken(email: String, refreshTokenOrPassword: String, signInSource: SignInSource, signInType: AccountConstants.SignInType): String {
        val properties = mapOf(KEY_SIGN_IN_SOURCE to signInSource.analyticsValue)
        try {
            val accessToken = when (signInType) {
                AccountConstants.SignInType.EmailPassword -> syncServerManager.login(email = email, password = refreshTokenOrPassword).token
                AccountConstants.SignInType.RefreshToken -> syncServerManager.loginToken(refreshToken = refreshTokenOrPassword).accessToken
            }
            analyticsTracker.track(AnalyticsEvent.USER_SIGNED_IN, properties)
            return accessToken
        } catch (ex: Exception) {
            analyticsTracker.track(AnalyticsEvent.USER_SIGNIN_FAILED, properties)
            throw ex
        }
    }

    private fun getResourceString(stringId: Int): String {
        return context.resources.getString(stringId)
    }

    sealed class AuthResult {
        data class Success(val result: AuthResultModel) : AuthResult()
        data class Failed(val message: String, val messageId: String?) : AuthResult()
    }
}

enum class SignInSource(val analyticsValue: String) {
    AccountAuthenticator("account_manager"),
    SignInViewModel("sign_in_view_model"),
    Onboarding("onboarding"),
    PocketCastsApplication("pocketcasts_application"),
}
