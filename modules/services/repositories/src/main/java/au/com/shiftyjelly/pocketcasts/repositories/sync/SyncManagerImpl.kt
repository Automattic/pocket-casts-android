package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.accounts.Account
import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.TracksAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.HistorySyncRequest
import au.com.shiftyjelly.pocketcasts.models.to.HistorySyncResponse
import au.com.shiftyjelly.pocketcasts.models.to.StatsBundle
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.preferences.AccountConstants
import au.com.shiftyjelly.pocketcasts.preferences.RefreshToken
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UploadProgressManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.toUploadData
import au.com.shiftyjelly.pocketcasts.servers.model.AuthResultModel
import au.com.shiftyjelly.pocketcasts.servers.sync.EpisodeSyncRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.FileAccount
import au.com.shiftyjelly.pocketcasts.servers.sync.FileImageUploadData
import au.com.shiftyjelly.pocketcasts.servers.sync.FilePost
import au.com.shiftyjelly.pocketcasts.servers.sync.FilesResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.NamedSettingsCaller
import au.com.shiftyjelly.pocketcasts.servers.sync.NamedSettingsRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.NamedSettingsResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.PodcastEpisodesResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.PodcastListResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.PromoCodeResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.ServerFile
import au.com.shiftyjelly.pocketcasts.servers.sync.SubscriptionPurchaseRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.SubscriptionStatusResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import au.com.shiftyjelly.pocketcasts.servers.sync.UpNextSyncRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.UpNextSyncResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.UserChangeResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.history.HistoryYearResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.login.ExchangeSonosResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.login.LoginTokenResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.parseErrorResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.update.SyncUpdateResponse
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.jakewharton.rxrelay2.BehaviorRelay
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.io.File
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Singleton
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Singleton
class SyncManagerImpl @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    @ApplicationContext private val context: Context,
    private val settings: Settings,
    private val syncAccountManager: SyncAccountManager,
    private val syncServerManager: SyncServerManager,
) : NamedSettingsCaller, SyncManager {

    override val isLoggedInObservable = BehaviorRelay.create<Boolean>().apply {
        accept(isLoggedIn())
    }

    companion object {
        private const val TRACKS_KEY_SIGN_IN_SOURCE = "sign_in_source"
        private const val TRACKS_KEY_ERROR_CODE = "error_code"
    }

// Account

    override fun emailChange(newEmail: String, password: String): Single<UserChangeResponse> =
        getCacheTokenOrLoginRxSingle { token ->
            syncServerManager.emailChange(newEmail, password, token)
        }.doOnSuccess {
            if (it.success == true) {
                syncAccountManager.setEmail(newEmail)
                analyticsTracker.track(AnalyticsEvent.USER_EMAIL_UPDATED)
            }
        }

    override fun deleteAccount(): Single<UserChangeResponse> =
        getCacheTokenOrLoginRxSingle { token ->
            syncServerManager.deleteAccount(token)
        }.doOnSuccess {
            if (it.success == true) {
                analyticsTracker.track(AnalyticsEvent.USER_ACCOUNT_DELETED)
            }
        }

    override suspend fun updatePassword(newPassword: String, oldPassword: String) {
        val response = getCacheTokenOrLogin { token ->
            val response = syncServerManager.updatePassword(newPassword, oldPassword, token)
            analyticsTracker.track(AnalyticsEvent.USER_PASSWORD_UPDATED)
            response
        }
        syncAccountManager.setRefreshToken(response.refreshToken)
        syncAccountManager.setAccessToken(response.accessToken)
    }

    override fun getUuid(): String? =
        syncAccountManager.getUuid()

    override fun isLoggedIn(): Boolean =
        syncAccountManager.isLoggedIn()

    override fun isGoogleLogin(): Boolean =
        syncAccountManager.isGoogleLogin()

    override fun getEmail(): String? =
        syncAccountManager.getEmail()

    override suspend fun getAccessToken(account: Account): AccessToken? =
        syncAccountManager.peekAccessToken(account)
            ?: fetchAccessToken(account)

    private suspend fun fetchAccessToken(account: Account): AccessToken? {
        val refreshToken = syncAccountManager.getRefreshToken(account) ?: return null
        return try {
            Timber.d("Refreshing the access token")
            val tokenResponse = downloadTokens(
                email = account.name,
                refreshToken = refreshToken,
                syncServerManager = syncServerManager,
                signInType = syncAccountManager.getSignInType(account),
                signInSource = SignInSource.AccountAuthenticator
            )
            // update the refresh token as the expiry may have been increased
            syncAccountManager.setRefreshToken(tokenResponse.refreshToken)
            tokenResponse.accessToken
        } catch (ex: Exception) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, ex, "Unable to fetch access token.")
            null
        }
    }

    override fun signOut(action: () -> Unit) {
        syncServerManager.signOut()
        action()
        syncAccountManager.signOut()
        isLoggedInObservable.accept(false)
    }

    override suspend fun loginWithGoogle(idToken: String, signInSource: SignInSource): LoginResult {
        val loginResult = try {
            val response = syncServerManager.loginGoogle(idToken)
            val result = handleTokenResponse(loginIdentity = LoginIdentity.Google, response = response)
            LoginResult.Success(result)
        } catch (ex: Exception) {
            Timber.e(ex, "Failed to sign in with Google")
            exceptionToAuthResult(exception = ex, fallbackMessage = LR.string.error_login_failed)
        }
        trackSignIn(loginResult, signInSource)
        return loginResult
    }

    override suspend fun loginWithEmailAndPassword(email: String, password: String, signInSource: SignInSource): LoginResult {
        val loginResult = try {
            val response = syncServerManager.login(email = email, password = password)
            val result = handleTokenResponse(loginIdentity = LoginIdentity.PocketCasts, response = response)
            LoginResult.Success(result)
        } catch (ex: Exception) {
            Timber.e(ex, "Failed to sign in with Pocket Casts")
            exceptionToAuthResult(exception = ex, fallbackMessage = LR.string.error_login_failed)
        }
        trackSignIn(loginResult, signInSource)
        return loginResult
    }

    override suspend fun createUserWithEmailAndPassword(email: String, password: String): LoginResult {
        val loginResult = try {
            val response = syncServerManager.register(email = email, password = password)
            val result = handleTokenResponse(loginIdentity = LoginIdentity.PocketCasts, response = response)
            LoginResult.Success(result)
        } catch (ex: Exception) {
            Timber.e(ex, "Failed to create a Pocket Casts account")
            exceptionToAuthResult(exception = ex, fallbackMessage = LR.string.server_login_unable_to_create_account)
        }
        trackRegister(loginResult)
        return loginResult
    }

    override suspend fun forgotPassword(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
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
            onError(context.resources.getString(LR.string.profile_reset_password_failed))
        }
    }

// User Episodes / Files

    override fun getFiles(): Single<Response<FilesResponse>> =
        getCacheTokenOrLoginRxSingle { token ->
            syncServerManager.getFiles(token)
        }

    override fun getFileUsage(): Single<FileAccount> =
        getCacheTokenOrLoginRxSingle { token ->
            syncServerManager.getFileUsage(token)
        }

    override fun postFiles(files: List<FilePost>): Single<Response<Void>> =
        getCacheTokenOrLoginRxSingle { token ->
            syncServerManager.postFiles(files, token)
        }

    override fun getFileUploadStatus(episodeUuid: String): Single<Boolean> =
        getCacheTokenOrLoginRxSingle { token ->
            syncServerManager.getFileUploadStatus(episodeUuid, token)
        }

    override fun uploadFileToServer(episode: UserEpisode): Completable =
        getCacheTokenOrLoginRxSingle { token ->
            syncServerManager.getFileUploadUrl(episode.toUploadData(), token)
        }.flatMapCompletable { url ->
            Timber.d("Upload url $url")
            syncServerManager.uploadToServer(episode, url)
                .doOnNext { progress ->
                    Timber.d("Progress $progress")
                    UploadProgressManager.uploadObservers[episode.uuid]?.forEach { consumer ->
                        consumer.accept(progress)
                    }
                }
                .ignoreElements()
        }

    override fun uploadImageToServer(
        episode: UserEpisode,
        imageFile: File
    ): Completable =
        getCacheTokenOrLoginRxSingle { token ->
            val imageData = FileImageUploadData(episode.uuid, imageFile.length(), "image/png")
            syncServerManager.getFileImageUploadUrl(imageData, token)
        }.flatMapCompletable { uploadUrl ->
            syncServerManager.uploadImageToServer(imageFile, uploadUrl)
                .ignoreElement()
        }

    override fun deleteImageFromServer(episode: UserEpisode): Single<Response<Void>> =
        getCacheTokenOrLoginRxSingle { token ->
            syncServerManager.deleteImageFromServer(episode, token)
        }

    override fun deleteFromServer(episode: UserEpisode): Single<Response<Void>> =
        getCacheTokenOrLoginRxSingle { token ->
            syncServerManager.deleteFromServer(episode, token)
        }

    override fun getPlaybackUrl(episode: UserEpisode): Single<String> =
        getCacheTokenOrLoginRxSingle { token ->
            syncServerManager.getPlaybackUrl(episode, token)
        }

    override fun getUserEpisode(uuid: String): Maybe<ServerFile> =
        getCacheTokenOrLoginRxSingle { token ->
            syncServerManager.getUserEpisode(uuid, token)
        }.flatMapMaybe {
            if (it.isSuccessful) {
                Maybe.just(it.body())
            } else if (it.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                Maybe.empty()
            } else {
                Maybe.error(HttpException(it))
            }
        }

// History

    override fun historySync(request: HistorySyncRequest): Single<HistorySyncResponse> =
        getCacheTokenOrLoginRxSingle { token ->
            syncServerManager.historySync(request, token)
        }

    override suspend fun historyYear(year: Int, count: Boolean): HistoryYearResponse =
        getCacheTokenOrLogin { token ->
            syncServerManager.historyYear(year, count, token)
        }

// Subscription

    override fun subscriptionStatus(): Single<SubscriptionStatusResponse> =
        getCacheTokenOrLoginRxSingle { token ->
            syncServerManager.subscriptionStatus(token)
        }

    override fun subscriptionPurchase(request: SubscriptionPurchaseRequest): Single<SubscriptionStatusResponse> =
        getCacheTokenOrLoginRxSingle { token ->
            syncServerManager.subscriptionPurchase(request, token)
        }

    override fun redeemPromoCode(code: String): Single<PromoCodeResponse> =
        getCacheTokenOrLoginRxSingle { token ->
            syncServerManager.redeemPromoCode(code, token)
        }

    override fun validatePromoCode(code: String): Single<PromoCodeResponse> =
        syncServerManager.validatePromoCode(code)

// Sync

    override fun syncUpdate(data: String, lastModified: String): Single<SyncUpdateResponse> =
        getEmail()?.let { email ->
            getCacheTokenOrLoginRxSingle { token ->
                syncServerManager.syncUpdate(email, data, lastModified, token)
            }
        } ?: Single.error(Exception("Not logged in"))

    override fun getLastSyncAt(): Single<String> =
        getCacheTokenOrLoginRxSingle { token ->
            syncServerManager.getLastSyncAt(token)
        }

    override fun getHomeFolder(): Single<PodcastListResponse> =
        getCacheTokenOrLoginRxSingle { token ->
            syncServerManager.getHomeFolder(token)
        }

    override fun getPodcastEpisodes(podcastUuid: String): Single<PodcastEpisodesResponse> =
        getCacheTokenOrLoginRxSingle { token ->
            syncServerManager.getPodcastEpisodes(podcastUuid, token)
        }

    override fun episodeSync(request: EpisodeSyncRequest): Completable =
        getCacheTokenOrLoginRxCompletable { token ->
            syncServerManager.episodeSync(request, token)
        }

// Other

    override suspend fun exchangeSonos(): ExchangeSonosResponse =
        syncServerManager.exchangeSonos()

    override fun getFilters(): Single<List<Playlist>> =
        getCacheTokenOrLoginRxSingle { token ->
            syncServerManager.getFilters(token)
        }

    override suspend fun loadStats(): StatsBundle =
        getCacheTokenOrLogin { token ->
            syncServerManager.loadStats(token)
        }

    override suspend fun namedSettings(request: NamedSettingsRequest): NamedSettingsResponse =
        getCacheTokenOrLogin { token ->
            syncServerManager.namedSettings(request, token)
        }

    override fun upNextSync(request: UpNextSyncRequest): Single<UpNextSyncResponse> =
        getCacheTokenOrLoginRxSingle { token ->
            syncServerManager.upNextSync(request, token)
        }

// private methods

    private fun exceptionToAuthResult(exception: Exception, fallbackMessage: Int): LoginResult.Failed {
        val resources = context.resources
        var message: String? = null
        var messageId: String? = null
        if (exception is HttpException) {
            val errorResponse = exception.parseErrorResponse()
            message = errorResponse?.messageLocalized(resources)
            messageId = errorResponse?.messageId
        }
        message = message ?: resources.getString(fallbackMessage)
        return LoginResult.Failed(message = message, messageId = messageId)
    }

    private fun trackSignIn(loginResult: LoginResult, signInSource: SignInSource) {
        val properties = mapOf(TRACKS_KEY_SIGN_IN_SOURCE to signInSource.analyticsValue)
        when (loginResult) {
            is LoginResult.Success -> {
                analyticsTracker.track(AnalyticsEvent.USER_SIGNED_IN, properties)
            }
            is LoginResult.Failed -> {
                val errorCodeValue = loginResult.messageId ?: TracksAnalyticsTracker.INVALID_OR_NULL_VALUE
                val errorProperties = properties.plus(TRACKS_KEY_ERROR_CODE to errorCodeValue)
                analyticsTracker.track(AnalyticsEvent.USER_SIGNIN_FAILED, errorProperties)
            }
        }
    }

    private fun trackRegister(loginResult: LoginResult) {
        when (loginResult) {
            is LoginResult.Success -> {
                analyticsTracker.track(AnalyticsEvent.USER_ACCOUNT_CREATED)
            }
            is LoginResult.Failed -> {
                val errorCodeValue = loginResult.messageId ?: TracksAnalyticsTracker.INVALID_OR_NULL_VALUE
                analyticsTracker.track(
                    AnalyticsEvent.USER_ACCOUNT_CREATION_FAILED,
                    mapOf(
                        TRACKS_KEY_ERROR_CODE to errorCodeValue
                    )
                )
            }
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

    private suspend fun <T : Any> getCacheTokenOrLogin(serverCall: suspend (token: AccessToken) -> T): T {
        if (isLoggedIn()) {
            return try {
                val token = syncAccountManager.getAccessToken() ?: refreshTokenSuspend()
                serverCall(token)
            } catch (ex: Exception) {
                // refresh invalid
                if (isHttpUnauthorized(ex)) {
                    val token = refreshTokenSuspend()
                    serverCall(token)
                } else {
                    throw ex
                }
            }
        } else {
            val token = refreshTokenSuspend()
            return serverCall(token)
        }
    }

    private fun <T : Any> getCacheTokenOrLoginRxSingle(serverCall: (token: AccessToken) -> Single<T>): Single<T> {
        if (isLoggedIn()) {
            return Single.fromCallable {
                runBlocking { syncAccountManager.getAccessToken() }
                    ?: throw RuntimeException("Failed to get token")
            }
                .flatMap { token -> serverCall(token) }
                // refresh invalid
                .onErrorResumeNext { throwable ->
                    return@onErrorResumeNext if (isHttpUnauthorized(throwable)) {
                        refreshToken().flatMap { token -> serverCall(token) }
                    }
                    // re-throw this error because it's not recoverable from here
                    else {
                        Single.error(throwable)
                    }
                }
        } else {
            return refreshToken().flatMap { token -> serverCall(token) }
        }
    }

    private fun getCacheTokenOrLoginRxCompletable(serverCall: (token: AccessToken) -> Completable): Completable {
        return getCacheTokenOrLoginRxSingle { token ->
            serverCall(token).toSingleDefault(Unit)
        }.ignoreElement()
    }

    private fun refreshToken(): Single<AccessToken> {
        syncAccountManager.invalidateAccessToken()
        return Single.fromCallable {
            runBlocking {
                syncAccountManager.getAccessToken()
            } ?: throw RuntimeException("Failed to get token")
        }.doOnError {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, it, "Refresh token threw an error.")
        }
    }

    private suspend fun refreshTokenSuspend(): AccessToken {
        syncAccountManager.invalidateAccessToken()
        return syncAccountManager.getAccessToken() ?: throw Exception("Failed to get refresh token")
    }

    private fun isHttpUnauthorized(throwable: Throwable?): Boolean {
        return throwable is HttpException && throwable.code() == 401
    }

    private fun handleTokenResponse(loginIdentity: LoginIdentity, response: LoginTokenResponse): AuthResultModel {
        val email = response.email
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Signed in successfully to $email")
        // Store details in android account manager
        syncAccountManager.addAccount(
            email = email,
            uuid = response.uuid,
            refreshToken = response.refreshToken,
            accessToken = response.accessToken,
            loginIdentity = loginIdentity
        )
        isLoggedInObservable.accept(true)

        settings.setLastModified(null)

        return AuthResultModel(
            token = response.accessToken,
            uuid = response.uuid,
            isNewAccount = response.isNew
        )
    }
}
