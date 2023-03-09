package au.com.shiftyjelly.pocketcasts.servers.account

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AccountManagerFuture
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.TracksAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.preferences.AccountConstants
import au.com.shiftyjelly.pocketcasts.preferences.RefreshToken
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.model.AuthResultModel
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import au.com.shiftyjelly.pocketcasts.servers.sync.login.LoginTokenResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.parseErrorResponse
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.jakewharton.rxrelay2.BehaviorRelay
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class SyncAccountManagerImpl @Inject constructor(
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    @ApplicationContext private val context: Context
) : SyncAccountManager {

    companion object {
        private const val TRACKS_KEY_SIGN_IN_SOURCE = "sign_in_source"
        private const val TRACKS_KEY_ERROR_CODE = "error_code"
    }

    private val accountManager = AccountManager.get(context)
    private var lastSignInErrorNotification: Long? = null

    override val isLoggedInObservable = BehaviorRelay.create<Boolean>().apply { accept(isLoggedIn()) }

    private fun getAccount(): Account? {
        return accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE).firstOrNull()
    }

    override fun isLoggedIn(): Boolean {
        return getAccount() != null
    }

    override fun getEmail(): String? {
        return getAccount()?.name
    }

    override fun getUuid(): String? {
        return getAccount()?.let { account ->
            accountManager.getUserData(account, AccountConstants.UUID)
        }
    }

    override fun peekAccessToken(account: Account?): AccessToken? {
        val accountFound = account ?: getAccount() ?: return null
        return accountManager.peekAuthToken(accountFound, AccountConstants.TOKEN_TYPE)?.let {
            if (it.isNotEmpty()) {
                AccessToken(it)
            } else {
                null
            }
        }
    }

    // TODO can this be nested in getAccessTokenSuspend
    private suspend fun getAccessToken(onErrorIntent: (Intent) -> Unit): AccessToken? {
        val account = getAccount() ?: return null
        return withContext(Dispatchers.IO) {
            try {
                val resultFuture: AccountManagerFuture<Bundle> = accountManager.getAuthToken(
                    account,
                    AccountConstants.TOKEN_TYPE,
                    Bundle(),
                    false,
                    null,
                    null
                )
                val bundle: Bundle = resultFuture.result // This call will block until the result is available.
                val token = bundle.getString(AccountManager.KEY_AUTHTOKEN)
                // Token failed to refresh
                if (token == null) {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        bundle.getParcelable(AccountManager.KEY_INTENT, Intent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        bundle.getParcelable(AccountManager.KEY_INTENT) as? Intent
                    }
                    intent?.let { onErrorIntent(it) }
                    throw SecurityException("Token could not be refreshed")
                } else {
                    AccessToken(token)
                }
            } catch (e: Exception) {
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "Could not get token")
                throw e // Rethrow the exception so it carries on
            }
        }
    }

    override fun getAccessTokenBlocking(onTokenErrorUiShown: () -> Unit): AccessToken? = runBlocking {
        getAccessTokenSuspend(onTokenErrorUiShown)
    }

    override suspend fun getAccessTokenSuspend(onTokenErrorUiShown: () -> Unit): AccessToken? {
        val onErrorIntent: (Intent) -> Unit = { intent -> showSignInErrorNotification(intent, onTokenErrorUiShown) }
        return getAccessToken(onErrorIntent)
    }

    override fun invalidateAccessToken() {
        val accessToken = peekAccessToken() ?: return
        accountManager.invalidateAuthToken(AccountConstants.ACCOUNT_TYPE, accessToken.value)
    }

    private fun getSignInType(account: Account): AccountConstants.SignInType {
        return AccountConstants.SignInType.fromString(accountManager.getUserData(account, AccountConstants.SIGN_IN_TYPE_KEY))
    }

    private fun addAccount(email: String, uuid: String, refreshToken: RefreshToken, accessToken: AccessToken) {
        val account = Account(email, AccountConstants.ACCOUNT_TYPE)
        val userData = bundleOf(
            AccountConstants.UUID to uuid,
            AccountConstants.SIGN_IN_TYPE_KEY to AccountConstants.SignInType.Tokens.value
        )
        accountManager.addAccountExplicitly(account, refreshToken.value, userData)
        accountManager.setAuthToken(account, AccountConstants.TOKEN_TYPE, accessToken.value)
        isLoggedInObservable.accept(true)
    }

    override fun signOut() {
        accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE).forEach { account ->
            accountManager.removeAccountExplicitly(account)
        }
        isLoggedInObservable.accept(false)
    }

    override fun updateEmail(email: String) {
        val account = getAccount() ?: return
        accountManager.renameAccount(account, email, null, null)
    }

    override fun setRefreshToken(refreshToken: RefreshToken) {
        val account = getAccount() ?: return
        accountManager.setPassword(account, refreshToken.value)
    }

    override fun setAccessToken(accessToken: AccessToken) {
        val account = getAccount() ?: return
        accountManager.setAuthToken(account, AccountConstants.TOKEN_TYPE, accessToken.value)
    }

    override fun getRefreshToken(account: Account): RefreshToken? {
        return accountManager.getPassword(account)?.let {
            if (it.isNotEmpty()) {
                RefreshToken(it)
            } else {
                null
            }
        }
    }

    override suspend fun signInWithGoogle(idToken: String, syncServerManager: SyncServerManager, signInSource: SignInSource): SignInResult {
        val signInResult = try {
            val response = syncServerManager.loginGoogle(idToken)
            val result = handleTokenResponse(response)
            SignInResult.Success(result)
        } catch (ex: Exception) {
            Timber.e(ex, "Failed to sign in with Google")
            exceptionToAuthResult(exception = ex, fallbackMessage = LR.string.error_login_failed)
        }
        trackSignIn(signInResult, signInSource)
        return signInResult
    }

    override suspend fun signInWithEmailAndPassword(email: String, password: String, syncServerManager: SyncServerManager, signInSource: SignInSource): SignInResult {
        val signInResult = try {
            val response = syncServerManager.login(email = email, password = password)
            val result = handleTokenResponse(response)
            SignInResult.Success(result)
        } catch (ex: Exception) {
            Timber.e(ex, "Failed to sign in with Pocket Casts")
            exceptionToAuthResult(exception = ex, fallbackMessage = LR.string.error_login_failed)
        }
        trackSignIn(signInResult, signInSource)
        return signInResult
    }

    private fun trackSignIn(signInResult: SignInResult, signInSource: SignInSource) {
        val properties = mapOf(TRACKS_KEY_SIGN_IN_SOURCE to signInSource.analyticsValue)
        when (signInResult) {
            is SignInResult.Success -> {
                analyticsTracker.track(AnalyticsEvent.USER_SIGNED_IN, properties)
            }
            is SignInResult.Failed -> {
                val errorCodeValue = signInResult.messageId ?: TracksAnalyticsTracker.INVALID_OR_NULL_VALUE
                val errorProperties = properties.plus(TRACKS_KEY_ERROR_CODE to errorCodeValue)
                analyticsTracker.track(AnalyticsEvent.USER_SIGNIN_FAILED, errorProperties)
            }
        }
    }

    override suspend fun createUserWithEmailAndPassword(email: String, password: String, syncServerManager: SyncServerManager): SignInResult {
        val signInResult = try {
            val response = syncServerManager.register(email = email, password = password)
            val result = handleTokenResponse(response)
            SignInResult.Success(result)
        } catch (ex: Exception) {
            Timber.e(ex, "Failed to create a Pocket Casts account")
            exceptionToAuthResult(exception = ex, fallbackMessage = LR.string.server_login_unable_to_create_account)
        }
        trackRegister(signInResult)
        return signInResult
    }

    private fun trackRegister(signInResult: SignInResult) {
        when (signInResult) {
            is SignInResult.Success -> {
                analyticsTracker.track(AnalyticsEvent.USER_ACCOUNT_CREATED)
            }
            is SignInResult.Failed -> {
                val errorCodeValue = signInResult.messageId ?: TracksAnalyticsTracker.INVALID_OR_NULL_VALUE
                analyticsTracker.track(
                    AnalyticsEvent.USER_ACCOUNT_CREATION_FAILED,
                    mapOf(
                        TRACKS_KEY_ERROR_CODE to errorCodeValue
                    )
                )
            }
        }
    }

    private fun exceptionToAuthResult(exception: Exception, fallbackMessage: Int): SignInResult.Failed {
        val resources = context.resources
        var message: String? = null
        var messageId: String? = null
        if (exception is HttpException) {
            val errorResponse = exception.parseErrorResponse()
            message = errorResponse?.messageLocalized(resources)
            messageId = errorResponse?.messageId
        }
        message = message ?: resources.getString(fallbackMessage)
        return SignInResult.Failed(message = message, messageId = messageId)
    }

    override suspend fun forgotPassword(email: String, syncServerManager: SyncServerManager, onSuccess: () -> Unit, onError: (String) -> Unit) {
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

    private fun handleTokenResponse(response: LoginTokenResponse): AuthResultModel {
        val email = response.email
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Signed in successfully to $email")
        // Store details in android account manager
        addAccount(
            email = email,
            uuid = response.uuid,
            refreshToken = response.refreshToken,
            accessToken = response.accessToken
        )

        settings.setLastModified(null)

        return AuthResultModel(token = response.accessToken, uuid = response.uuid, isNewAccount = response.isNew)
    }

    override suspend fun refreshAccessToken(account: Account?, syncServerManager: SyncServerManager): AccessToken? {
        val accountFound = account ?: getAccount() ?: return null
        val refreshToken = getRefreshToken(accountFound) ?: return null
        return try {
            Timber.d("Refreshing the access token")
            val tokenResponse = downloadTokens(
                email = accountFound.name,
                refreshToken = refreshToken,
                syncServerManager = syncServerManager,
                signInType = getSignInType(accountFound),
                signInSource = SignInSource.AccountAuthenticator
            )
            // update the refresh token as the expiry may have been increased
            accountManager.setPassword(accountFound, tokenResponse.refreshToken.value)
            tokenResponse.accessToken
        } catch (ex: Exception) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, ex, "Unable to refresh token.")
            null
        }
    }

    private suspend fun downloadTokens(
        email: String,
        refreshToken: RefreshToken,
        syncServerManager: SyncServerManager,
        signInSource: SignInSource,
        signInType: AccountConstants.SignInType
    ): LoginTokenResponse {
        val properties = mapOf(TRACKS_KEY_SIGN_IN_SOURCE to signInSource.analyticsValue)
        try {
            val response = when (signInType) {
                AccountConstants.SignInType.Password -> syncServerManager.login(email = email, password = refreshToken.value)
                AccountConstants.SignInType.Tokens -> syncServerManager.loginToken(refreshToken = refreshToken)
            }
            analyticsTracker.track(AnalyticsEvent.USER_SIGNED_IN, properties)
            return response
        } catch (ex: Exception) {
            analyticsTracker.track(AnalyticsEvent.USER_SIGNIN_FAILED, properties)
            throw ex
        }
    }

    private fun getResourceString(stringId: Int): String {
        return context.resources.getString(stringId)
    }

    private fun showSignInErrorNotification(intent: Intent, onTokenErrorUiShown: () -> Unit) {
        onShowSignInErrorNotificationDebounced(onTokenErrorUiShown)

        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT.or(PendingIntent.FLAG_IMMUTABLE))
        val notification = NotificationCompat.Builder(context, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_SIGN_IN_ERROR.id)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentTitle(context.getString(LR.string.token_refresh_sign_in_error_title))
            .setContentText(context.getString(LR.string.token_refresh_sign_in_error_description))
            .setAutoCancel(true)
            // TODO fix this!!!!!!
            // .setSmallIcon(IR.drawable.ic_failedwarning)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context)
            .notify(Settings.NotificationId.SIGN_IN_ERROR.value, notification)
    }

    // Avoid invoking the passed function multiple times in a short period of time
    @Synchronized
    private fun onShowSignInErrorNotificationDebounced(onTokenErrorUiShown: () -> Unit) {
        val now = System.currentTimeMillis()
        // Do not invoke this method more than once every 2 seconds
        val shouldInvoke = lastSignInErrorNotification == null ||
            lastSignInErrorNotification!! < now - (2 * 1000)
        if (shouldInvoke) {
            onTokenErrorUiShown()
        }
        lastSignInErrorNotification = now
    }

    override fun setEmail(email: String) {
        val account = getAccount() ?: return
        accountManager.renameAccount(account, email, null, null)
    }
}

enum class SignInSource(val analyticsValue: String) {
    AccountAuthenticator("account_manager"),
    SignInViewModel("sign_in_view_model"),
    Onboarding("onboarding"),
    PocketCastsApplication("pocketcasts_application"),
}
