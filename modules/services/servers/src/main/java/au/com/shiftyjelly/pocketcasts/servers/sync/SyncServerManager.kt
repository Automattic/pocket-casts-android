package au.com.shiftyjelly.pocketcasts.servers.sync

import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.HistorySyncRequest
import au.com.shiftyjelly.pocketcasts.models.to.HistorySyncResponse
import au.com.shiftyjelly.pocketcasts.models.to.StatsBundle
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.preferences.RefreshToken
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.di.SyncServerCache
import au.com.shiftyjelly.pocketcasts.servers.di.SyncServerRetrofit
import au.com.shiftyjelly.pocketcasts.servers.sync.forgotpassword.ForgotPasswordRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.forgotpassword.ForgotPasswordResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.history.HistoryYearResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.history.HistoryYearSyncRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.login.ExchangeSonosResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.login.LoginGoogleRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.login.LoginRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.login.LoginTokenRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.login.LoginTokenResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.register.RegisterRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.update.SyncUpdateResponse
import au.com.shiftyjelly.pocketcasts.utils.extensions.parseIsoDate
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import retrofit2.Retrofit
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only class outside of the server module that should use this class is the
 * repository module's SyncManager class. Consider using that instead of this class.
 */
@Singleton
open class SyncServerManager @Inject constructor(
    @SyncServerRetrofit retrofit: Retrofit,
    val settings: Settings,
    @SyncServerCache val cache: Cache,
) {

    companion object {
        const val SCOPE_MOBILE = "mobile"
    }

    private val server: SyncServer = retrofit.create(SyncServer::class.java)

    suspend fun register(email: String, password: String): LoginTokenResponse {
        val request = RegisterRequest(email = email, password = password, scope = SCOPE_MOBILE)
        return server.register(request)
    }

    suspend fun login(email: String, password: String): LoginTokenResponse {
        val request = LoginRequest(email = email, password = password, scope = SCOPE_MOBILE)
        return server.login(request)
    }

    suspend fun loginGoogle(idToken: String): LoginTokenResponse {
        val request = LoginGoogleRequest(idToken = idToken, scope = SCOPE_MOBILE)
        return server.loginGoogle(request)
    }

    suspend fun loginToken(refreshToken: RefreshToken): LoginTokenResponse {
        val request = LoginTokenRequest(refreshToken = refreshToken)
        return server.loginToken(request)
    }

    suspend fun forgotPassword(email: String): ForgotPasswordResponse {
        val request = ForgotPasswordRequest(email = email)
        return server.forgotPassword(request)
    }

    suspend fun exchangeSonos(token: AccessToken): ExchangeSonosResponse {
        return server.exchangeSonos(addBearer(token))
    }

    fun emailChange(newEmail: String, password: String, token: AccessToken): Single<UserChangeResponse> {
        val request = EmailChangeRequest(
            newEmail,
            password,
            SCOPE_MOBILE
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

    suspend fun namedSettings(request: NamedSettingsRequest, token: AccessToken): NamedSettingsResponse =
        server.namedSettings(addBearer(token), request)

    fun syncUpdate(email: String, data: String, lastModified: String, token: AccessToken): Single<SyncUpdateResponse> {
        val fields = mapOf(
            "email" to email,
            "token" to token.value,
            "data" to data,
            "device_utc_time_ms" to System.currentTimeMillis().toString(),
            "last_modified" to lastModified
        )
        return server.syncUpdate(fields)
    }

    fun upNextSync(request: UpNextSyncRequest, token: AccessToken): Single<UpNextSyncResponse> =
        server.upNextSync(addBearer(token), request)

    fun getLastSyncAt(token: AccessToken): Single<String> =
        server.getLastSyncAt(addBearer(token), buildBasicRequest())
            .map { response -> response.lastSyncAt ?: "" }

    fun getHomeFolder(token: AccessToken): Single<PodcastListResponse> =
        server.getPodcastList(addBearer(token), buildBasicRequest()).map { response ->
            response.copy(podcasts = removeHomeFolderUuid(response.podcasts), folders = response.folders)
        }

    private fun removeHomeFolderUuid(podcasts: List<PodcastResponse>?): List<PodcastResponse>? =
        podcasts?.map { podcast ->
            if (podcast.folderUuid != null && podcast.folderUuid == Folder.homeFolderUuid) {
                podcast.copy(folderUuid = null)
            } else {
                podcast
            }
        }

    fun getPodcastEpisodes(podcastUuid: String, token: AccessToken): Single<PodcastEpisodesResponse> {
        val request = PodcastEpisodesRequest(podcastUuid)
        return server.getPodcastEpisodes(addBearer(token), request)
    }

    fun getFilters(token: AccessToken): Single<List<Playlist>> =
        server.getFilterList(addBearer(token), buildBasicRequest())
            .map { response -> response.filters?.mapNotNull { it.toFilter() } ?: emptyList() }

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
        token: AccessToken
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
            BackpressureStrategy.LATEST
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
        Single.just("${Settings.SERVER_API_URL}/files/url/${episode.uuid}?token=$token")

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

    fun signOut() {
        cache.evictAll()
    }

    private fun buildBasicRequest(): BasicRequest {
        return BasicRequest(
            model = Settings.SYNC_API_MODEL,
            version = Settings.SYNC_API_VERSION
        )
    }

    private fun addBearer(token: AccessToken): String {
        return "Bearer ${token.value}"
    }
}
