package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.accounts.Account
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.HistorySyncRequest
import au.com.shiftyjelly.pocketcasts.models.to.HistorySyncResponse
import au.com.shiftyjelly.pocketcasts.models.to.StatsBundle
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.preferences.RefreshToken
import au.com.shiftyjelly.pocketcasts.servers.sync.EpisodeSyncRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.FileAccount
import au.com.shiftyjelly.pocketcasts.servers.sync.FilePost
import au.com.shiftyjelly.pocketcasts.servers.sync.FilesResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.LoginIdentity
import au.com.shiftyjelly.pocketcasts.servers.sync.NamedSettingsCaller
import au.com.shiftyjelly.pocketcasts.servers.sync.PodcastEpisodesResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.PromoCodeResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.ServerFile
import au.com.shiftyjelly.pocketcasts.servers.sync.SubscriptionPurchaseRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.SubscriptionStatusResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.UpNextSyncRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.UpNextSyncResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.UserChangeResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.history.HistoryYearResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.login.ExchangeSonosResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.update.SyncUpdateResponse
import au.com.shiftyjelly.pocketcasts.utils.Optional
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
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import java.io.File
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import retrofit2.Response
import com.pocketcasts.service.api.SyncUpdateRequest as SyncUpdateProtoRequest
import com.pocketcasts.service.api.SyncUpdateResponse as SyncUpdateProtoResponse

interface SyncManager : NamedSettingsCaller {

    // Account
    val isLoggedInObservable: BehaviorRelay<Boolean>
    fun isGoogleLogin(): Boolean
    fun isLoggedIn(): Boolean
    fun getLoginIdentity(): LoginIdentity?
    fun getEmail(): String?
    fun emailFlow(): Flow<String?>
    fun emailFlowable(): Flowable<Optional<String>>
    fun signOut(action: () -> Unit = {})
    suspend fun loginWithGoogle(idToken: String, signInSource: SignInSource): LoginResult
    suspend fun loginWithEmailAndPassword(email: String, password: String, signInSource: SignInSource): LoginResult
    suspend fun loginWithToken(token: RefreshToken, loginIdentity: LoginIdentity, signInSource: SignInSource): LoginResult
    suspend fun createUserWithEmailAndPassword(email: String, password: String): LoginResult
    suspend fun forgotPassword(email: String, onSuccess: () -> Unit, onError: (String) -> Unit)
    suspend fun getAccessToken(account: Account): AccessToken
    fun getRefreshToken(): RefreshToken?
    suspend fun emailChange(newEmail: String, password: String): UserChangeResponse
    fun deleteAccountRxSingle(): Single<UserChangeResponse>
    suspend fun updatePassword(newPassword: String, oldPassword: String)
    suspend fun <T> getCacheTokenOrLogin(serverCall: suspend (token: AccessToken) -> T): T

    // User Episodes / Files
    fun getFilesRxSingle(): Single<Response<FilesResponse>>
    fun getFileUploadStatusRxSingle(episodeUuid: String): Single<Boolean>
    fun uploadFileToServerRxCompletable(episode: UserEpisode): Completable
    fun uploadImageToServerRxCompletable(episode: UserEpisode, imageFile: File): Completable
    fun postFilesRxSingle(files: List<FilePost>): Single<Response<Void>>
    fun getUserEpisodeRxMaybe(uuid: String): Maybe<ServerFile>
    fun getFileUsageRxSingle(): Single<FileAccount>
    fun deleteImageFromServerRxSingle(episode: UserEpisode): Single<Response<Void>>
    fun deleteFromServerRxSingle(episode: UserEpisode): Single<Response<Void>>
    fun getPlaybackUrlRxSingle(episode: UserEpisode): Single<String>

    // History
    fun historySyncRxSingle(request: HistorySyncRequest): Single<HistorySyncResponse>
    suspend fun historyYear(year: Int, count: Boolean): HistoryYearResponse

    // Subscription
    suspend fun subscriptionStatus(): SubscriptionStatusResponse
    suspend fun subscriptionPurchase(request: SubscriptionPurchaseRequest): SubscriptionStatusResponse
    fun redeemPromoCodeRxSingle(code: String): Single<PromoCodeResponse>
    fun validatePromoCodeRxSingle(code: String): Single<PromoCodeResponse>

    // Sync
    fun getLastSyncAtRxSingle(): Single<String>
    suspend fun getLastSyncAtOrThrow(): String
    suspend fun getHomeFolderOrThrow(): UserPodcastListResponse
    suspend fun getPlaylistsOrThrow(): UserPlaylistListResponse
    suspend fun getBookmarksOrThrow(): BookmarksResponse
    suspend fun getEpisodesOrThrow(request: PodcastsEpisodesRequest): EpisodesResponse
    fun getPodcastEpisodesRxSingle(podcastUuid: String): Single<PodcastEpisodesResponse>

    suspend fun syncUpdate(data: String, lastSyncTime: Instant): SyncUpdateResponse
    suspend fun syncUpdateOrThrow(request: SyncUpdateProtoRequest): SyncUpdateProtoResponse

    fun episodeSyncRxCompletable(request: EpisodeSyncRequest): Completable

    // Rating
    suspend fun addPodcastRating(podcastUuid: String, rate: Int): PodcastRatingResponse
    suspend fun getPodcastRating(podcastUuid: String): PodcastRatingResponse
    suspend fun getPodcastRatings(): PodcastRatingsResponse?

    // Other
    suspend fun exchangeSonos(): ExchangeSonosResponse
    suspend fun getFilters(): List<PlaylistEntity>
    suspend fun loadStats(): StatsBundle
    suspend fun upNextSync(request: UpNextSyncRequest): UpNextSyncResponse
    suspend fun getBookmarks(): List<Bookmark>
    suspend fun sendAnonymousFeedback(subject: String, inbox: String, message: String): Response<Void>
    suspend fun sendFeedback(subject: String, inbox: String, message: String): Response<Void>

    // Referral
    suspend fun getReferralCode(): Response<ReferralCodeResponse>
    suspend fun getWinbackOffer(): Response<WinbackResponse>
    suspend fun validateReferralCode(code: String): Response<ReferralValidationResponse>
    suspend fun redeemReferralCode(code: String): Response<ReferralRedemptionResponse>
}
