package au.com.shiftyjelly.pocketcasts.servers.sync

import android.os.Build
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.HistorySyncRequest
import au.com.shiftyjelly.pocketcasts.models.to.HistorySyncResponse
import au.com.shiftyjelly.pocketcasts.models.to.StatsBundle
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.preferences.RefreshToken
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.di.Cached
import au.com.shiftyjelly.pocketcasts.servers.di.SyncServerRetrofit
import au.com.shiftyjelly.pocketcasts.servers.sync.bookmark.toBookmark
import au.com.shiftyjelly.pocketcasts.servers.sync.forgotpassword.ForgotPasswordRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.forgotpassword.ForgotPasswordResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.history.HistoryYearResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.history.HistoryYearSyncRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.login.ExchangeSonosResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.login.LoginGoogleRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.login.LoginPocketCastsRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.login.LoginTokenRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.login.LoginTokenResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.register.RegisterRequest
import au.com.shiftyjelly.pocketcasts.utils.extensions.parseIsoDate
import com.pocketcasts.service.api.PodcastRatingAddRequest
import com.pocketcasts.service.api.PodcastRatingResponse
import com.pocketcasts.service.api.PodcastRatingShowRequest
import com.pocketcasts.service.api.SupportFeedbackRequest
import com.pocketcasts.service.api.SyncUpdateRequest
import com.pocketcasts.service.api.SyncUpdateResponse
import com.pocketcasts.service.api.UserPodcastListResponse
import com.pocketcasts.service.api.bookmarkRequest
import com.pocketcasts.service.api.userPodcastListRequest
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import java.io.File
import java.time.Instant
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import retrofit2.Retrofit

/**
 * The only class outside of the server module that should use this class is the
 * repository module's SyncManager class. Consider using that instead of this class.
 */
@Singleton
open class SyncServerManager @Inject constructor(
    @SyncServerRetrofit retrofit: Retrofit,
    val settings: Settings,
    @Cached val cache: Cache,
) {

    companion object {
        const val SCOPE_MOBILE = "mobile"

        private val userPodcastListRequest = userPodcastListRequest {
            v = Settings.SYNC_API_VERSION.toString()
            m = Settings.SYNC_API_MODEL
        }
    }

    private val server: SyncServer = retrofit.create(SyncServer::class.java)

    suspend fun register(email: String, password: String): LoginTokenResponse {
        val request = RegisterRequest(email = email, password = password, scope = SCOPE_MOBILE)
        return server.register(request)
    }

    suspend fun login(email: String, password: String): LoginTokenResponse {
        val request = LoginPocketCastsRequest(email = email, password = password, scope = SCOPE_MOBILE)
        return server.loginPocketCasts(request)
    }

    suspend fun loginGoogle(idToken: String): LoginTokenResponse {
        val request = LoginGoogleRequest(idToken = idToken, scope = SCOPE_MOBILE)
        return server.loginGoogle(request)
    }

    /**
     * Update the access token using the refresh token.
     * If any 4xx is returned the user should be logged out and asked to login.
     */
    suspend fun loginToken(refreshToken: RefreshToken): LoginTokenResponse {
        val request = LoginTokenRequest(refreshToken = refreshToken, scope = SCOPE_MOBILE)
        return server.loginToken(request)
    }

    suspend fun forgotPassword(email: String): ForgotPasswordResponse {
        val request = ForgotPasswordRequest(email = email)
        return server.forgotPassword(request)
    }

    suspend fun exchangeSonos(token: AccessToken): ExchangeSonosResponse {
        return server.exchangeSonos(addBearer(token))
    }

    suspend fun emailChange(newEmail: String, password: String, token: AccessToken): UserChangeResponse {
        val request = EmailChangeRequest(
            newEmail,
            password,
            SCOPE_MOBILE,
        )
        return server.emailChange(addBearer(token), request)
    }

    fun deleteAccount(token: AccessToken): Single<UserChangeResponse> =
        server.deleteAccount(addBearer(token))

    suspend fun updatePassword(newPassword: String, oldPassword: String, token: AccessToken): LoginTokenResponse {
        val request = UpdatePasswordRequest(newPassword = newPassword, oldPassword = oldPassword, scope = SCOPE_MOBILE)
        return server.updatePassword(authorization = addBearer(token), request = request)
    }

    fun redeemPromoCode(code: String, token: AccessToken): Single<PromoCodeResponse> {
        val request = PromoCodeRequest(code)
        return server.redeemPromoCode(addBearer(token), request)
    }

    fun validatePromoCode(code: String): Single<PromoCodeResponse> =
        server.validatePromoCode(PromoCodeRequest(code))

    @Suppress("DEPRECATION")
    @Deprecated("This method can be removed when the sync settings feature flag is removed")
    suspend fun namedSettings(request: NamedSettingsRequest, token: AccessToken): NamedSettingsResponse =
        server.namedSettings(addBearer(token), request)

    suspend fun changedNamedSettings(request: ChangedNamedSettingsRequest, token: AccessToken): ChangedNamedSettingsResponse =
        server.namedSettings(addBearer(token), request)

    @Deprecated("This should no longer be used once the SETTINGS_SYNC feature flag is removed/permanently-enabled.")
    fun syncUpdate(email: String, data: String, lastSyncTime: Instant, token: AccessToken): Single<au.com.shiftyjelly.pocketcasts.servers.sync.update.SyncUpdateResponse> {
        val fields = mutableMapOf(
            "email" to email,
            "token" to token.value,
            "data" to data,
            "device_utc_time_ms" to System.currentTimeMillis().toString(),
            "last_modified" to lastSyncTime.toString(),
        )
        addDeviceFields(fields)

        @Suppress("DEPRECATION")
        return server.syncUpdate(fields)
    }

    suspend fun userSyncUpdate(
        token: AccessToken,
        request: SyncUpdateRequest,
    ): SyncUpdateResponse =
        server.userSyncUpdate(addBearer(token), request)

    fun upNextSync(request: UpNextSyncRequest, token: AccessToken): Single<UpNextSyncResponse> =
        server.upNextSync(addBearer(token), request)

    fun getLastSyncAt(token: AccessToken): Single<String> =
        server.getLastSyncAt(addBearer(token), buildBasicRequest())
            .map { response -> response.lastSyncAt ?: "" }

    suspend fun getHomeFolder(token: AccessToken): UserPodcastListResponse =
        server.getPodcastList(addBearer(token), userPodcastListRequest)

    fun getPodcastEpisodes(podcastUuid: String, token: AccessToken): Single<PodcastEpisodesResponse> {
        val request = PodcastEpisodesRequest(podcastUuid)
        return server.getPodcastEpisodes(addBearer(token), request)
    }

    fun getFilters(token: AccessToken): Single<List<Playlist>> =
        server.getFilterList(addBearer(token), buildBasicRequest())
            .map { response -> response.filters?.mapNotNull { it.toFilter() } ?: emptyList() }

    suspend fun getBookmarks(token: AccessToken): List<Bookmark> {
        return server.getBookmarkList(addBearer(token), bookmarkRequest {}).bookmarksList.map { it.toBookmark() }
    }

    fun historySync(request: HistorySyncRequest, token: AccessToken): Single<HistorySyncResponse> =
        server.historySync(addBearer(token), request)

    /**
     * Retrieve listening history for a year.
     * @param year The year to get the user's listening history from.
     * @param count When true only returns a count instead of the full list of episodes.
     */
    suspend fun historyYear(year: Int, count: Boolean, token: AccessToken): HistoryYearResponse {
        val request = HistoryYearSyncRequest(count = count, year = year)
        return server.historyYear(addBearer(token), request)
    }

    fun episodeSync(request: EpisodeSyncRequest, token: AccessToken): Completable =
        server.episodeProgressSync(addBearer(token), request)

    fun subscriptionStatus(token: AccessToken): Single<SubscriptionStatusResponse> =
        server.subscriptionStatus(addBearer(token))

    fun subscriptionPurchase(
        request: SubscriptionPurchaseRequest,
        token: AccessToken,
    ): Single<SubscriptionStatusResponse> =
        server.subscriptionPurchase(addBearer(token), request)

    fun getFiles(token: AccessToken): Single<Response<FilesResponse>> =
        server.getFiles(addBearer(token))

    fun postFiles(files: List<FilePost>, token: AccessToken): Single<Response<Void>> {
        val body = FilePostBody(files)
        return server.postFiles(addBearer(token), body)
    }

    fun getFileUploadUrl(file: FileUploadData, token: AccessToken): Single<String> =
        server.getFileUploadUrl(addBearer(token), file).map { it.url }

    fun getFileUploadStatus(episodeUuid: String, token: AccessToken): Single<Boolean> =
        server.getFileUploadStatus(addBearer(token), episodeUuid).map { it.success }

    fun getFileImageUploadUrl(imageData: FileImageUploadData, token: AccessToken): Single<String> =
        server.getFileImageUploadUrl(addBearer(token), imageData).map { it.url }

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
            BackpressureStrategy.LATEST,
        )
    }

    fun uploadImageToServer(imageFile: File, url: String): Single<Response<Void>> {
        val requestBody = imageFile.asRequestBody("image/png".toMediaType())
        return server.uploadFileNoProgress(url, requestBody)
    }

    fun deleteImageFromServer(episode: UserEpisode, token: AccessToken): Single<Response<Void>> =
        server.deleteImageFile(addBearer(token), episode.uuid)

    fun deleteFromServer(episode: UserEpisode, token: AccessToken): Single<Response<Void>> =
        server.deleteFile(addBearer(token), episode.uuid)

    fun getPlaybackUrl(episode: UserEpisode, token: AccessToken): Single<String> =
        Single.just("${Settings.SERVER_API_URL}/files/url/${episode.uuid}?token=${token.value}")

    fun getUserEpisode(uuid: String, token: AccessToken): Single<Response<ServerFile>> =
        server.getFile(addBearer(token), uuid)

    suspend fun loadStats(token: AccessToken): StatsBundle {
        val response = server.loadStats(addBearer(token), StatsSummaryRequest(deviceId = settings.getUniqueDeviceId()))
        // Convert the strings to a map of longs
        val values = response.filter { (it.value as? String)?.toLongOrNull() != null }.mapValues { (it.value as? String)?.toLong() ?: 0 }
        val startedAt = (response[StatsBundle.SERVER_KEY_STARTED_AT] as? String)?.parseIsoDate()
        return StatsBundle(values, startedAt)
    }

    fun getFileUsage(token: AccessToken): Single<FileAccount> =
        server.getFilesUsage(addBearer(token))

    suspend fun addPodcastRating(podcastUuid: String, rate: Int, token: AccessToken): PodcastRatingResponse {
        val request = PodcastRatingAddRequest.newBuilder()
            .setPodcastUuid(podcastUuid)
            .setPodcastRating(rate)
            .build()
        return server.addPodcastRating(addBearer(token), request)
    }

    suspend fun getPodcastRating(podcastUuid: String, token: AccessToken): PodcastRatingResponse {
        val request = PodcastRatingShowRequest.newBuilder()
            .setPodcastUuid(podcastUuid)
            .build()
        return server.getPodcastRating(addBearer(token), request)
    }

    suspend fun sendAnonymousFeedback(subject: String, inbox: String, message: String): Response<Void> {
        val request = SupportFeedbackRequest.newBuilder()
            .setSubject(subject)
            .setInbox(inbox)
            .setMessage(message)
            .build()
        return server.sendAnonymousFeedback(request)
    }

    suspend fun sendFeedback(subject: String, inbox: String, message: String, token: AccessToken): Response<Void> {
        val request = SupportFeedbackRequest.newBuilder()
            .setSubject(subject)
            .setInbox(inbox)
            .setMessage(message)
            .build()
        return server.sendFeedback(addBearer(token), request)
    }

    fun signOut() {
        cache.evictAll()
    }

    private fun buildBasicRequest(): BasicRequest {
        return BasicRequest(
            model = Settings.SYNC_API_MODEL,
            version = Settings.SYNC_API_VERSION,
        )
    }

    private fun addBearer(token: AccessToken): String {
        return "Bearer ${token.value}"
    }

    private fun addDeviceFields(fields: MutableMap<String, String>) {
        with(fields) {
            put("device", settings.getUniqueDeviceId())
            put("v", Settings.PARSER_VERSION)
            put("av", settings.getVersion())
            put("ac", settings.getVersionCode().toString())
            put("dt", "2")
            put("c", Locale.getDefault().country)
            put("l", Locale.getDefault().language)
            put("m", Build.MODEL)
        }
    }
}
