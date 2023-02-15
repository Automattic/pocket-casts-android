package au.com.shiftyjelly.pocketcasts.servers.sync

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.HistorySyncRequest
import au.com.shiftyjelly.pocketcasts.models.to.HistorySyncResponse
import au.com.shiftyjelly.pocketcasts.models.to.StatsBundle
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.di.OnTokenErrorUiShown
import au.com.shiftyjelly.pocketcasts.servers.di.SyncServerCache
import au.com.shiftyjelly.pocketcasts.servers.di.SyncServerRetrofit
import au.com.shiftyjelly.pocketcasts.servers.sync.forgotpassword.ForgotPasswordRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.forgotpassword.ForgotPasswordResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.history.HistoryYearResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.history.HistoryYearSyncRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.login.LoginGoogleRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.login.LoginRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.login.LoginResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.login.LoginTokenRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.login.LoginTokenResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.register.RegisterRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.register.RegisterResponse
import au.com.shiftyjelly.pocketcasts.utils.extensions.parseIsoDate
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import java.io.File
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class SyncServerManager @Inject constructor(
    @SyncServerRetrofit retrofit: Retrofit,
    val settings: Settings,
    @SyncServerCache val cache: Cache,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    @OnTokenErrorUiShown private val onTokenErrorUiShown: () -> Unit,
) {

    companion object {
        const val SCOPE_MOBILE = "mobile"
    }

    private val server: SyncServer = retrofit.create(SyncServer::class.java)

    suspend fun register(email: String, password: String): RegisterResponse {
        val request = RegisterRequest(email = email, password = password, scope = SCOPE_MOBILE)
        return server.register(request)
    }

    suspend fun login(email: String, password: String): LoginResponse {
        val request = LoginRequest(email = email, password = password, scope = SCOPE_MOBILE)
        return server.login(request)
    }

    suspend fun loginGoogle(idToken: String): LoginTokenResponse {
        val request = LoginGoogleRequest(idToken)
        return server.loginGoogle(request)
    }

    suspend fun loginToken(refreshToken: String): LoginTokenResponse {
        val request = LoginTokenRequest(refreshCode = refreshToken)
        return server.loginToken(request)
    }

    suspend fun forgotPassword(email: String): ForgotPasswordResponse {
        val request = ForgotPasswordRequest(email = email)
        return server.forgotPassword(request)
    }

    fun emailChange(newEmail: String, password: String): Single<UserChangeResponse> {
        return getCacheTokenOrLogin { token ->
            val request = EmailChangeRequest(
                newEmail,
                password,
                SCOPE_MOBILE
            )
            server.emailChange(addBearer(token), request)
        }.doOnSuccess {
            if (it.success == true) {
                analyticsTracker.track(AnalyticsEvent.USER_EMAIL_UPDATED)
            }
        }
    }

    fun deleteAccount(): Single<UserChangeResponse> =
        getCacheTokenOrLogin { token ->
            server.deleteAccount(addBearer(token))
        }.doOnSuccess {
            if (it.success == true) {
                analyticsTracker.track(AnalyticsEvent.USER_ACCOUNT_DELETED)
            }
        }

    fun pwdChange(pwdNew: String, pwdOld: String): Single<PwdChangeResponse> {
        return getCacheTokenOrLogin { token ->
            val request = PwdChangeRequest(pwdNew, pwdOld, SCOPE_MOBILE)
            server.pwdChange(addBearer(token), request)
        }.doOnSuccess {
            if (it.success == true) {
                analyticsTracker.track(AnalyticsEvent.USER_PASSWORD_UPDATED)
            }
        }
    }

    fun redeemPromoCode(code: String): Single<PromoCodeResponse> {
        return getCacheTokenOrLogin { token ->
            val request = PromoCodeRequest(code)
            server.redeemPromoCode(addBearer(token), request)
        }
    }

    fun validatePromoCode(code: String): Single<PromoCodeResponse> {
        return server.validatePromoCode(PromoCodeRequest(code))
    }

    suspend fun namedSettings(request: NamedSettingsRequest): NamedSettingsResponse {
        return getCacheTokenOrLoginSuspend { token ->
            server.namedSettings(addBearer(token), request)
        }
    }

    fun upNextSync(request: UpNextSyncRequest): Single<UpNextSyncResponse> {
        return getCacheTokenOrLogin { token ->
            server.upNextSync(addBearer(token), request)
        }
    }

    fun getLastSyncAt(): Single<String> {
        return getCacheTokenOrLogin<String> { token ->
            server.getLastSyncAt(addBearer(token), buildBasicRequest())
                .map { response -> response.lastSyncAt ?: "" }
        }
    }

    fun getHomeFolder(): Single<PodcastListResponse> {
        return getCacheTokenOrLogin { token ->
            server.getPodcastList(addBearer(token), buildBasicRequest()).map { response ->
                response.copy(podcasts = removeHomeFolderUuid(response.podcasts), folders = response.folders)
            }
        }
    }

    private fun removeHomeFolderUuid(podcasts: List<PodcastResponse>?): List<PodcastResponse>? {
        return podcasts?.map { podcast ->
            if (podcast.folderUuid != null && podcast.folderUuid == Folder.homeFolderUuid) {
                podcast.copy(folderUuid = null)
            } else {
                podcast
            }
        }
    }

    fun getPodcastEpisodes(podcastUuid: String): Single<PodcastEpisodesResponse> {
        return getCacheTokenOrLogin { token ->
            val request = PodcastEpisodesRequest(podcastUuid)
            server.getPodcastEpisodes(addBearer(token), request)
        }
    }

    fun getFilters(): Single<List<Playlist>> {
        return getCacheTokenOrLogin<List<Playlist>> { token ->
            server.getFilterList(addBearer(token), buildBasicRequest())
                .map { response -> response.filters?.mapNotNull { it.toFilter() } ?: emptyList() }
        }
    }

    fun historySync(request: HistorySyncRequest): Single<HistorySyncResponse> {
        return getCacheTokenOrLogin<HistorySyncResponse> { token ->
            server.historySync(addBearer(token), request)
        }
    }

    /**
     * Retrieve listening history for a year.
     * @param year The year to get the user's listening history from.
     * @param count When true only returns a count instead of the full list of episodes.
     */
    suspend fun historyYear(year: Int, count: Boolean): HistoryYearResponse {
        return getCacheTokenOrLoginSuspend { token ->
            val request = HistoryYearSyncRequest(count = count, year = year)
            server.historyYear(addBearer(token), request)
        }
    }

    fun episodeSync(request: EpisodeSyncRequest): Completable {
        return getCacheTokenOrLoginCompletable { token ->
            server.episodeProgressSync(addBearer(token), request)
        }
    }

    fun subscriptionStatus(): Single<SubscriptionStatusResponse> {
        return getCacheTokenOrLogin { token ->
            server.subscriptionStatus(addBearer(token))
        }
    }

    fun subscriptionPurchase(request: SubscriptionPurchaseRequest): Single<SubscriptionStatusResponse> {
        return getCacheTokenOrLogin { token ->
            server.subscriptionPurchase(addBearer(token), request)
        }
    }

    fun getFiles(): Single<Response<FilesResponse>> {
        return getCacheTokenOrLogin { token ->
            server.getFiles(addBearer(token))
        }
    }

    fun postFiles(files: List<FilePost>): Single<Response<Void>> {
        return getCacheTokenOrLogin { token ->
            server.postFiles(
                addBearer(token),
                FilePostBody(files = files)
            )
        }
    }

    fun getUploadUrl(file: FileUploadData): Single<String> {
        return getCacheTokenOrLogin { token ->
            server.getUploadUrl(addBearer(token), file).map { it.url }
        }
    }

    fun getFileUploadStatus(episodeUuid: String): Single<Boolean> {
        return getCacheTokenOrLogin { token ->
            server.getFileUploadStatus(addBearer(token), episodeUuid).map { it.success }
        }
    }

    fun getImageUploadUrl(imageData: FileImageUploadData): Single<String> {
        return getCacheTokenOrLogin { token ->
            server.getImageUploadUrl(addBearer(token), imageData).map { it.url }
        }
    }

    fun uploadToServer(episode: UserEpisode, url: String): Flowable<Float> {
        val path = episode.downloadedFilePath ?: throw IllegalStateException("File is not downloaded")
        val file = File(path)

        return Flowable.create(
            { emitter ->
                try {
                    val requestBody = ProgressRequestBody.create((episode.fileType ?: "audio/mp3").toMediaType(), file, emitter)
                    val call = server.uploadFile(url, requestBody)
                    emitter.setCancellable { call.cancel() }

                    call.execute()
                    if (!emitter.isCancelled) {
                        emitter.onComplete()
                    }
                } catch (e: java.lang.Exception) {
                    emitter.tryOnError(e)
                }
            },
            BackpressureStrategy.LATEST
        )
    }

    fun uploadImageToServer(imageFile: File, url: String): Single<Response<Void>> {
        val requestBody = imageFile.asRequestBody("image/png".toMediaType())
        return server.uploadFileNoProgress(url, requestBody)
    }

    fun deleteImageFromServer(episode: UserEpisode): Single<Response<Void>> {
        return getCacheTokenOrLogin { token ->
            server.deleteImageFile(addBearer(token), episode.uuid)
        }
    }

    fun deleteFromServer(episode: UserEpisode): Single<Response<Void>> {
        return getCacheTokenOrLogin { token ->
            server.deleteFile(addBearer(token), episode.uuid)
        }
    }

    fun getPlaybackUrl(episode: UserEpisode): Single<String> {
        return getCacheTokenOrLogin { token ->
            Single.just("${Settings.SERVER_API_URL}/files/url/${episode.uuid}?token=$token")
        }
    }

    fun getUserEpisode(uuid: String): Maybe<ServerFile> {
        return getCacheTokenOrLogin { token ->
            server.getFile(addBearer(token), uuid)
        }.flatMapMaybe {
            if (it.isSuccessful) {
                Maybe.just(it.body())
            } else if (it.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                Maybe.empty()
            } else {
                Maybe.error(HttpException(it))
            }
        }
    }

    suspend fun loadStats(): StatsBundle {
        return getCacheTokenOrLoginSuspend { token ->
            val response = server.loadStats(addBearer(token), StatsSummaryRequest(deviceId = settings.getUniqueDeviceId()))
            // Convert the strings to a map of longs
            val values = response.filter { (it.value as? String)?.toLongOrNull() != null }.mapValues { (it.value as? String)?.toLong() ?: 0 }
            val startedAt = (response[StatsBundle.SERVER_KEY_STARTED_AT] as? String)?.parseIsoDate()
            StatsBundle(values, startedAt)
        }
    }

    fun getFileUsage(): Single<FileAccount> {
        return getCacheTokenOrLogin { token ->
            server.getFilesUsage(addBearer(token))
        }
    }

    fun signOut() {
        cache.evictAll()
    }

    fun cancelSupporterSubscription(subscriptionUuid: String): Single<Response<Void>> {
        return getCacheTokenOrLogin {
            server.cancelSubscription(addBearer(it), SupporterCancelRequest(subscriptionUuid))
        }
    }

    private suspend fun <T : Any> getCacheTokenOrLoginSuspend(serverCall: suspend (token: String) -> T): T {
        if (settings.isLoggedIn()) {
            return try {
                val token = settings.getSyncTokenSuspend(onTokenErrorUiShown) ?: refreshTokenSuspend()
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

    private fun getCacheTokenOrLoginCompletable(serverCall: (token: String) -> Completable): Completable {
        return getCacheTokenOrLogin { token ->
            serverCall(token).toSingleDefault(Unit)
        }.ignoreElement()
    }

    private fun <T : Any> getCacheTokenOrLogin(serverCall: (token: String) -> Single<T>): Single<T> {
        if (settings.isLoggedIn()) {
            return Single.fromCallable { settings.getSyncToken(onTokenErrorUiShown) ?: throw RuntimeException("Failed to get token") }
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
        }

        return refreshToken().flatMap { token -> serverCall(token) }
    }

    private fun buildBasicRequest(): BasicRequest {
        return BasicRequest(
            model = Settings.SYNC_API_MODEL,
            version = Settings.SYNC_API_VERSION
        )
    }

    private suspend fun refreshTokenSuspend(): String {
        settings.invalidateToken()
        return settings.getSyncTokenSuspend(onTokenErrorUiShown) ?: throw Exception("Failed to get refresh token")
    }

    private fun refreshToken(): Single<String> {
        settings.invalidateToken()
        return Single.fromCallable { settings.getSyncToken(onTokenErrorUiShown) ?: throw RuntimeException("Failed to get token") }
            .doOnError {
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, it, "Refresh token threw an error.")
            }
    }

    private fun isHttpUnauthorized(throwable: Throwable?): Boolean {
        return throwable is HttpException && throwable.code() == 401
    }

    private fun addBearer(token: String): String {
        return "Bearer $token"
    }
}
