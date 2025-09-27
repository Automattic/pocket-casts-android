package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.accounts.Account
import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.TracksAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.HistorySyncRequest
import au.com.shiftyjelly.pocketcasts.models.to.HistorySyncResponse
import au.com.shiftyjelly.pocketcasts.models.to.StatsBundle
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.preferences.AccountConstants
import au.com.shiftyjelly.pocketcasts.preferences.RefreshToken
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationManager
import au.com.shiftyjelly.pocketcasts.repositories.notification.OnboardingNotificationType
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UploadProgressManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.toUploadData
import au.com.shiftyjelly.pocketcasts.servers.model.AuthResultModel
import au.com.shiftyjelly.pocketcasts.servers.sync.EpisodeSyncRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.FileAccount
import au.com.shiftyjelly.pocketcasts.servers.sync.FileImageUploadData
import au.com.shiftyjelly.pocketcasts.servers.sync.FilePost
import au.com.shiftyjelly.pocketcasts.servers.sync.FilesResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.LoginIdentity
import au.com.shiftyjelly.pocketcasts.servers.sync.NamedSettingsCaller
import au.com.shiftyjelly.pocketcasts.servers.sync.NamedSettingsRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.NamedSettingsResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.PodcastEpisodesRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.PodcastEpisodesResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.PromoCodeResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.ServerFile
import au.com.shiftyjelly.pocketcasts.servers.sync.SubscriptionPurchaseRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.SubscriptionStatusResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServiceManager
import au.com.shiftyjelly.pocketcasts.servers.sync.UpNextSyncRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.UpNextSyncResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.UserChangeResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.bookmark.toBookmark
import au.com.shiftyjelly.pocketcasts.servers.sync.exception.RefreshTokenExpiredException
import au.com.shiftyjelly.pocketcasts.servers.sync.history.HistoryYearResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.login.ExchangeSonosResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.login.LoginTokenResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.parseErrorResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.update.SyncUpdateResponse
import au.com.shiftyjelly.pocketcasts.utils.Optional
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.jakewharton.rxrelay2.BehaviorRelay
import com.pocketcasts.service.api.BookmarksResponse
import com.pocketcasts.service.api.EpisodesResponse
import com.pocketcasts.service.api.PodcastRatingResponse
import com.pocketcasts.service.api.PodcastRatingsResponse
import com.pocketcasts.service.api.PodcastsEpisodesRequest
import com.pocketcasts.service.api.ReferralCodeResponse
import com.pocketcasts.service.api.ReferralRedemptionResponse
import com.pocketcasts.service.api.ReferralValidationResponse
import com.pocketcasts.service.api.UserPlaylistListResponse
import com.pocketcasts.service.api.UserPodcastListResponse
import com.pocketcasts.service.api.WinbackResponse
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import java.io.File
import java.net.HttpURLConnection
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.rx2.rxSingle
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import com.pocketcasts.service.api.SyncUpdateRequest as SyncUpdateProtoRequest
import com.pocketcasts.service.api.SyncUpdateResponse as SyncUpdateProtoResponse

@Singleton
class SyncManagerImpl @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
    @ApplicationContext private val context: Context,
    private val settings: Settings,
    private val syncAccountManager: SyncAccountManager,
    private val syncServiceManager: SyncServiceManager,
    private val moshi: Moshi,
    private val notificationManager: NotificationManager,
) : NamedSettingsCaller,
    SyncManager {

    override val isLoggedInObservable = BehaviorRelay.create<Boolean>().apply {
        accept(isLoggedIn())
    }

    companion object {
        private const val TRACKS_KEY_SOURCE_IN_CODE = "source_in_code"
        private const val TRACKS_KEY_SOURCE = "source"
        private const val TRACKS_KEY_ERROR_CODE = "error_code"
    }

// Account

    override suspend fun emailChange(newEmail: String, password: String): UserChangeResponse {
        val result = getCacheTokenOrLogin { token ->
            syncServiceManager.emailChange(newEmail, password, token)
        }
        if (result.success == true) {
            syncAccountManager.setEmail(newEmail)
            analyticsTracker.track(AnalyticsEvent.USER_EMAIL_UPDATED)
        }
        return result
    }

    override fun deleteAccountRxSingle(): Single<UserChangeResponse> = getCacheTokenOrLoginRxSingle { token ->
        syncServiceManager.deleteAccount(token)
    }.doOnSuccess {
        if (it.success == true) {
            analyticsTracker.track(AnalyticsEvent.USER_ACCOUNT_DELETED)
        }
    }

    override suspend fun updatePassword(newPassword: String, oldPassword: String) {
        val response = getCacheTokenOrLogin { token ->
            val response = syncServiceManager.updatePassword(newPassword, oldPassword, token)
            analyticsTracker.track(AnalyticsEvent.USER_PASSWORD_UPDATED)
            response
        }
        syncAccountManager.setRefreshToken(response.refreshToken)
        syncAccountManager.setAccessToken(response.accessToken)
    }

    override fun isLoggedIn(): Boolean = syncAccountManager.isLoggedIn()

    override fun isGoogleLogin(): Boolean = getLoginIdentity() == LoginIdentity.Google

    override fun getLoginIdentity(): LoginIdentity? = syncAccountManager.getLoginIdentity()

    override fun getEmail(): String? = syncAccountManager.getEmail()

    override fun emailFlow() = syncAccountManager.emailFlow().distinctUntilChanged()

    override fun emailFlowable(): Flowable<Optional<String>> = syncAccountManager.emailFlowable().distinctUntilChanged()

    override suspend fun getAccessToken(account: Account): AccessToken = syncAccountManager.peekAccessToken(account)
        ?: fetchAccessToken(account)

    override fun getRefreshToken(): RefreshToken? = syncAccountManager.getRefreshToken()

    private suspend fun fetchAccessToken(account: Account): AccessToken {
        val refreshToken = syncAccountManager.getRefreshToken(account) ?: throw RefreshTokenExpiredException()
        return try {
            val signInType = syncAccountManager.getSignInType(account)
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Fetching the access token, SignInType: $signInType")
            val tokenResponse = downloadTokens(
                email = account.name,
                refreshToken = refreshToken,
                signInType = signInType,
            )

            // update the refresh token as the expiry may have been increased
            syncAccountManager.setRefreshToken(tokenResponse.refreshToken)

            syncAccountManager.setAccessToken(tokenResponse.accessToken)
            tokenResponse.accessToken
        } catch (ex: Exception) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, ex, "Unable to fetch access token.")
            if (isHttpClientError(ex)) {
                throw RefreshTokenExpiredException()
            } else {
                throw ex
            }
        }
    }

    override fun signOut(action: () -> Unit) {
        syncServiceManager.signOut()
        action()
        syncAccountManager.signOut()
        isLoggedInObservable.accept(false)
    }

    override suspend fun loginWithGoogle(
        idToken: String,
        signInSource: SignInSource,
    ): LoginResult = handleLogin(signInSource, LoginIdentity.Google) {
        syncServiceManager.loginGoogle(idToken)
    }

    override suspend fun loginWithToken(
        token: RefreshToken,
        loginIdentity: LoginIdentity,
        signInSource: SignInSource,
    ): LoginResult = handleLogin(signInSource, loginIdentity) {
        syncServiceManager.loginToken(token)
    }

    override suspend fun loginWithEmailAndPassword(
        email: String,
        password: String,
        signInSource: SignInSource,
    ): LoginResult = handleLogin(signInSource, LoginIdentity.PocketCasts) {
        syncServiceManager.login(email = email, password = password)
    }

    private suspend fun handleLogin(
        signInSource: SignInSource,
        loginIdentity: LoginIdentity,
        loginFunction: suspend () -> LoginTokenResponse,
    ): LoginResult {
        val loginResult = try {
            val response = loginFunction()
            val result = handleTokenResponse(loginIdentity = loginIdentity, response = response)
            LoginResult.Success(result)
        } catch (ex: Exception) {
            Timber.e(ex, "Failed to sign in with Pocket Casts")
            exceptionToAuthResult(exception = ex, fallbackMessage = LR.string.error_login_failed)
        }

        trackSignIn(loginResult, signInSource, loginIdentity)

        return loginResult
    }

    override suspend fun createUserWithEmailAndPassword(email: String, password: String): LoginResult {
        val loginResult = try {
            val response = syncServiceManager.register(email = email, password = password)
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
            val response = syncServiceManager.forgotPassword(email = email)
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

    override fun getFilesRxSingle(): Single<Response<FilesResponse>> = getCacheTokenOrLoginRxSingle { token ->
        syncServiceManager.getFiles(token)
    }

    override fun getFileUsageRxSingle(): Single<FileAccount> = getCacheTokenOrLoginRxSingle { token ->
        syncServiceManager.getFileUsage(token)
    }

    override fun postFilesRxSingle(files: List<FilePost>): Single<Response<Void>> = getCacheTokenOrLoginRxSingle { token ->
        syncServiceManager.postFiles(files, token)
    }

    override fun getFileUploadStatusRxSingle(episodeUuid: String): Single<Boolean> = getCacheTokenOrLoginRxSingle { token ->
        syncServiceManager.getFileUploadStatus(episodeUuid, token)
    }

    override fun uploadFileToServerRxCompletable(episode: UserEpisode): Completable = getCacheTokenOrLoginRxSingle { token ->
        syncServiceManager.getFileUploadUrl(episode.toUploadData(), token)
    }.flatMapCompletable { url ->
        syncServiceManager.uploadToServer(episode, url)
            .doOnNext { progress -> UploadProgressManager.pushProgress(episode.uuid, progress) }
            .ignoreElements()
    }

    override fun uploadImageToServerRxCompletable(
        episode: UserEpisode,
        imageFile: File,
    ): Completable = getCacheTokenOrLoginRxSingle { token ->
        val imageData = FileImageUploadData(episode.uuid, imageFile.length(), "image/png")
        syncServiceManager.getFileImageUploadUrl(imageData, token)
    }.flatMapCompletable { uploadUrl ->
        syncServiceManager.uploadImageToServer(imageFile, uploadUrl)
            .ignoreElement()
    }

    override fun deleteImageFromServerRxSingle(episode: UserEpisode): Single<Response<Void>> = getCacheTokenOrLoginRxSingle { token ->
        syncServiceManager.deleteImageFromServer(episode, token)
    }

    override fun deleteFromServerRxSingle(episode: UserEpisode): Single<Response<Void>> = getCacheTokenOrLoginRxSingle { token ->
        syncServiceManager.deleteFromServer(episode, token)
    }

    override fun getPlaybackUrlRxSingle(episode: UserEpisode): Single<String> = getCacheTokenOrLoginRxSingle { token ->
        syncServiceManager.getPlaybackUrl(episode, token)
    }

    override fun getUserEpisodeRxMaybe(uuid: String): Maybe<ServerFile> = getCacheTokenOrLoginRxSingle { token ->
        syncServiceManager.getUserEpisode(uuid, token)
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

    override fun historySyncRxSingle(request: HistorySyncRequest): Single<HistorySyncResponse> = getCacheTokenOrLoginRxSingle { token ->
        syncServiceManager.historySync(request, token)
    }

    override suspend fun historyYear(year: Int, count: Boolean): HistoryYearResponse = getCacheTokenOrLogin { token ->
        syncServiceManager.historyYear(year, count, token)
    }

// Subscription

    override suspend fun subscriptionStatus(): SubscriptionStatusResponse = getCacheTokenOrLogin { token ->
        syncServiceManager.subscriptionStatus(token)
    }

    override suspend fun subscriptionPurchase(request: SubscriptionPurchaseRequest): SubscriptionStatusResponse = getCacheTokenOrLogin { token ->
        syncServiceManager.subscriptionPurchase(request, token)
    }

    override fun redeemPromoCodeRxSingle(code: String): Single<PromoCodeResponse> = getCacheTokenOrLoginRxSingle { token ->
        syncServiceManager.redeemPromoCode(code, token)
    }

    override fun validatePromoCodeRxSingle(code: String): Single<PromoCodeResponse> = syncServiceManager.validatePromoCode(code)

// Sync

    override suspend fun syncUpdate(data: String, lastSyncTime: Instant): SyncUpdateResponse = getEmail()?.let { email ->
        getCacheTokenOrLogin { token ->
            syncServiceManager.syncUpdate(email, data, lastSyncTime, token)
        }
    } ?: throw Exception("Not logged in")

    override suspend fun syncUpdateOrThrow(request: SyncUpdateProtoRequest): SyncUpdateProtoResponse = getCacheTokenOrLogin { token ->
        syncServiceManager.syncUpdateOrThrow(token, request)
    }

    override fun getLastSyncAtRxSingle(): Single<String> = getCacheTokenOrLoginRxSingle { token ->
        syncServiceManager.getLastSyncAtRx(token)
    }

    override suspend fun getLastSyncAtOrThrow(): String = getCacheTokenOrLogin { token ->
        syncServiceManager.getLastSyncAtOrThrow(token)
    }

    override suspend fun getHomeFolderOrThrow(): UserPodcastListResponse = getCacheTokenOrLogin { token ->
        syncServiceManager.getHomeFolder(token)
    }

    override suspend fun getPlaylistsOrThrow(): UserPlaylistListResponse = getCacheTokenOrLogin { token ->
        syncServiceManager.getPlaylists(token)
    }

    override suspend fun getBookmarksOrThrow(): BookmarksResponse = getCacheTokenOrLogin { token ->
        syncServiceManager.getBookmarks(token)
    }

    override suspend fun getEpisodesOrThrow(request: PodcastsEpisodesRequest): EpisodesResponse = getCacheTokenOrLogin { token ->
        syncServiceManager.getEpisodes(request, token)
    }

    override fun getPodcastEpisodesRxSingle(podcastUuid: String): Single<PodcastEpisodesResponse> = getCacheTokenOrLoginRxSingle { token ->
        syncServiceManager.getPodcastEpisodes(podcastUuid, token)
    }

    override fun episodeSyncRxCompletable(request: EpisodeSyncRequest): Completable = getCacheTokenOrLoginRxCompletable { token ->
        syncServiceManager.episodeSync(request, token)
    }

    // Rating

    override suspend fun addPodcastRating(podcastUuid: String, rate: Int): PodcastRatingResponse = getCacheTokenOrLogin { token ->
        syncServiceManager.addPodcastRating(podcastUuid, rate, token)
    }

    override suspend fun getPodcastRating(podcastUuid: String): PodcastRatingResponse = getCacheTokenOrLogin { token ->
        syncServiceManager.getPodcastRating(podcastUuid, token)
    }

    override suspend fun getPodcastRatings(): PodcastRatingsResponse? = getCacheTokenOrLogin { token ->
        syncServiceManager.getPodcastRatings(token)
    }

    // Other

    override suspend fun exchangeSonos(): ExchangeSonosResponse = getCacheTokenOrLogin { token ->
        syncServiceManager.exchangeSonos(token)
    }

    override suspend fun getFilters(): List<PlaylistEntity> = getCacheTokenOrLogin { token ->
        syncServiceManager.getFilters(token)
    }

    override suspend fun getBookmarks(): List<Bookmark> {
        return getCacheTokenOrLogin { token ->
            syncServiceManager.getBookmarks(token).bookmarksList.map { it.toBookmark() }
        }
    }

    override suspend fun sendAnonymousFeedback(subject: String, inbox: String, message: String): Response<Void> {
        return syncServiceManager.sendAnonymousFeedback(subject, inbox, message)
    }

    override suspend fun sendFeedback(subject: String, inbox: String, message: String): Response<Void> = getCacheTokenOrLogin { token ->
        syncServiceManager.sendFeedback(subject, inbox, message, token)
    }

    override suspend fun loadStats(): StatsBundle = getCacheTokenOrLogin { token ->
        syncServiceManager.loadStats(token)
    }

    override suspend fun namedSettings(
        request: NamedSettingsRequest,
    ): NamedSettingsResponse = getCacheTokenOrLogin { token ->
        syncServiceManager.namedSettings(request, token)
    }

    override suspend fun upNextSync(request: UpNextSyncRequest): UpNextSyncResponse = getCacheTokenOrLogin { token ->
        syncServiceManager.upNextSync(request, token)
    }

    // Referral
    override suspend fun getReferralCode(): Response<ReferralCodeResponse> {
        return getCacheTokenOrLogin { token ->
            syncServiceManager.getReferralCode(token)
        }
    }

    override suspend fun getWinbackOffer(): Response<WinbackResponse> {
        return getCacheTokenOrLogin { token ->
            syncServiceManager.getWinbackOffer(token)
        }
    }

    override suspend fun validateReferralCode(code: String): Response<ReferralValidationResponse> {
        return getCacheTokenOrLogin { token ->
            syncServiceManager.validateReferralCode(token, code)
        }
    }

    override suspend fun redeemReferralCode(code: String): Response<ReferralRedemptionResponse> {
        return getCacheTokenOrLogin { token ->
            syncServiceManager.redeemReferralCode(token, code)
        }
    }

// private methods

    private fun exceptionToAuthResult(exception: Exception, fallbackMessage: Int): LoginResult.Failed {
        val resources = context.resources
        var message: String? = null
        var messageId: String? = null
        if (exception is HttpException) {
            val errorResponse = exception.parseErrorResponse(moshi)
            message = errorResponse?.messageLocalized(resources)
            messageId = errorResponse?.messageId
        }
        message = message ?: resources.getString(fallbackMessage)
        return LoginResult.Failed(message = message, messageId = messageId)
    }

    private suspend fun trackSignIn(
        loginResult: LoginResult,
        signInSource: SignInSource,
        loginIdentity: LoginIdentity,
    ) {
        val source = when (loginIdentity) {
            LoginIdentity.Google -> "google"
            LoginIdentity.PocketCasts -> "password"
        }
        when (loginResult) {
            is LoginResult.Success -> {
                when (signInSource) {
                    SignInSource.WatchPhoneSync ->
                        analyticsTracker.track(AnalyticsEvent.USER_SIGNED_IN_WATCH_FROM_PHONE)

                    is SignInSource.UserInitiated -> {
                        analyticsTracker.track(
                            event = if (loginResult.result.isNewAccount) {
                                AnalyticsEvent.USER_ACCOUNT_CREATED
                            } else {
                                AnalyticsEvent.USER_SIGNED_IN
                            },
                            properties = mapOf(
                                TRACKS_KEY_SOURCE to source,
                                TRACKS_KEY_SOURCE_IN_CODE to signInSource.analyticsValue,
                            ),
                        )
                        if (loginResult.result.isNewAccount) {
                            notificationManager.updateUserFeatureInteraction(OnboardingNotificationType.Sync)
                        }
                    }
                }
            }

            is LoginResult.Failed -> {
                val errorCodeValue = loginResult.messageId ?: TracksAnalyticsTracker.INVALID_OR_NULL_VALUE
                when (signInSource) {
                    SignInSource.WatchPhoneSync ->
                        analyticsTracker.track(
                            AnalyticsEvent.USER_SIGNIN_WATCH_FROM_PHONE_FAILED,
                            mapOf(
                                TRACKS_KEY_ERROR_CODE to errorCodeValue,
                            ),
                        )

                    is SignInSource.UserInitiated ->
                        analyticsTracker.track(
                            AnalyticsEvent.USER_SIGNIN_FAILED,
                            mapOf(
                                TRACKS_KEY_SOURCE to source,
                                TRACKS_KEY_SOURCE_IN_CODE to signInSource.analyticsValue,
                                TRACKS_KEY_ERROR_CODE to errorCodeValue,
                            ),
                        )
                }
            }
        }
    }

    private suspend fun trackRegister(loginResult: LoginResult) {
        when (loginResult) {
            is LoginResult.Success -> {
                analyticsTracker.track(
                    AnalyticsEvent.USER_ACCOUNT_CREATED,
                    mapOf(TRACKS_KEY_SOURCE to "password"), // This method is only used when creating an account with a password
                )
                notificationManager.updateUserFeatureInteraction(OnboardingNotificationType.Sync)
            }

            is LoginResult.Failed -> {
                val errorCodeValue = loginResult.messageId ?: TracksAnalyticsTracker.INVALID_OR_NULL_VALUE
                analyticsTracker.track(
                    AnalyticsEvent.USER_ACCOUNT_CREATION_FAILED,
                    mapOf(
                        TRACKS_KEY_ERROR_CODE to errorCodeValue,
                    ),
                )
            }
        }
    }

    private suspend fun downloadTokens(
        email: String,
        refreshToken: RefreshToken,
        signInType: AccountConstants.SignInType,
    ): LoginTokenResponse = when (signInType) {
        AccountConstants.SignInType.Password -> syncServiceManager.login(email = email, password = refreshToken.value)
        AccountConstants.SignInType.Tokens -> syncServiceManager.loginToken(refreshToken = refreshToken)
    }

    override suspend fun <T> getCacheTokenOrLogin(serverCall: suspend (token: AccessToken) -> T): T {
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
            return rxSingle {
                syncAccountManager.getAccessToken() ?: throw RuntimeException("Failed to get token")
            }
                .flatMap { token -> serverCall(token) }
                // refresh invalid
                .onErrorResumeNext { throwable ->
                    return@onErrorResumeNext if (isHttpUnauthorized(throwable)) {
                        refreshTokenRxSingle().flatMap { token -> serverCall(token) }
                    }
                    // re-throw this error because it's not recoverable from here
                    else {
                        Single.error(throwable)
                    }
                }
        } else {
            return refreshTokenRxSingle().flatMap { token -> serverCall(token) }
        }
    }

    private fun getCacheTokenOrLoginRxCompletable(serverCall: (token: AccessToken) -> Completable): Completable {
        return getCacheTokenOrLoginRxSingle { token ->
            serverCall(token).toSingleDefault(Unit)
        }.ignoreElement()
    }

    private fun refreshTokenRxSingle(): Single<AccessToken> {
        syncAccountManager.invalidateAccessToken()
        return rxSingle {
            syncAccountManager.getAccessToken() ?: throw RuntimeException("Failed to get token")
        }.doOnError {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, it, "Refresh token threw an error.")
        }
    }

    private suspend fun refreshTokenSuspend(): AccessToken {
        syncAccountManager.invalidateAccessToken()
        return syncAccountManager.getAccessToken() ?: throw Exception("Failed to get access token")
    }

    private fun isHttpUnauthorized(throwable: Throwable?): Boolean {
        return throwable is HttpException && throwable.code() == 401
    }

    private fun isHttpClientError(throwable: Throwable?): Boolean {
        return throwable is HttpException && throwable.code() in 400..499
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
            loginIdentity = loginIdentity,
        )
        isLoggedInObservable.accept(true)

        settings.setFullySignedOut(false)
        settings.setLastModified(null)

        return AuthResultModel(
            token = response.accessToken,
            uuid = response.uuid,
            isNewAccount = response.isNew,
        )
    }
}
