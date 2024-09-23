package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.accounts.Account
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
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
import com.jakewharton.rxrelay2.BehaviorRelay
import com.pocketcasts.service.api.PodcastRatingResponse
import com.pocketcasts.service.api.ReferralCodeResponse
import com.pocketcasts.service.api.ReferralRedemptionResponse
import com.pocketcasts.service.api.ReferralValidationResponse
import com.pocketcasts.service.api.UserPodcastListResponse
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import java.io.File
import java.time.Instant
import retrofit2.Response

interface SyncManager : NamedSettingsCaller {

    // Account
    val isLoggedInObservable: BehaviorRelay<Boolean>
    fun isGoogleLogin(): Boolean
    fun isLoggedIn(): Boolean
    fun getLoginIdentity(): LoginIdentity?
    fun getEmail(): String?
    fun signOut(action: () -> Unit = {})
    suspend fun loginWithGoogle(idToken: String, signInSource: SignInSource): LoginResult
    suspend fun loginWithEmailAndPassword(email: String, password: String, signInSource: SignInSource): LoginResult
    suspend fun loginWithToken(token: RefreshToken, loginIdentity: LoginIdentity, signInSource: SignInSource): LoginResult
    suspend fun createUserWithEmailAndPassword(email: String, password: String): LoginResult
    suspend fun forgotPassword(email: String, onSuccess: () -> Unit, onError: (String) -> Unit)
    suspend fun getAccessToken(account: Account): AccessToken
    fun getRefreshToken(): RefreshToken?
    suspend fun emailChange(newEmail: String, password: String): UserChangeResponse
    fun deleteAccount(): Single<UserChangeResponse>
    suspend fun updatePassword(newPassword: String, oldPassword: String)

    // User Episodes / Files
    fun getFiles(): Single<Response<FilesResponse>>
    fun getFileUploadStatus(episodeUuid: String): Single<Boolean>
    fun uploadFileToServer(episode: UserEpisode): Completable
    fun uploadImageToServer(episode: UserEpisode, imageFile: File): Completable
    fun postFiles(files: List<FilePost>): Single<Response<Void>>
    fun getUserEpisode(uuid: String): Maybe<ServerFile>
    fun getFileUsage(): Single<FileAccount>
    fun deleteImageFromServer(episode: UserEpisode): Single<Response<Void>>
    fun deleteFromServer(episode: UserEpisode): Single<Response<Void>>
    fun getPlaybackUrl(episode: UserEpisode): Single<String>

    // History
    fun historySync(request: HistorySyncRequest): Single<HistorySyncResponse>
    suspend fun historyYear(year: Int, count: Boolean): HistoryYearResponse

    // Subscription
    fun subscriptionStatus(): Single<SubscriptionStatusResponse>
    fun subscriptionPurchase(request: SubscriptionPurchaseRequest): Single<SubscriptionStatusResponse>
    fun redeemPromoCode(code: String): Single<PromoCodeResponse>
    fun validatePromoCode(code: String): Single<PromoCodeResponse>

    // Sync
    fun getLastSyncAt(): Single<String>
    suspend fun getHomeFolder(): UserPodcastListResponse
    fun getPodcastEpisodes(podcastUuid: String): Single<PodcastEpisodesResponse>

    fun syncUpdate(data: String, lastSyncTime: Instant): Single<SyncUpdateResponse>

    fun episodeSync(request: EpisodeSyncRequest): Completable

    // Rating
    suspend fun addPodcastRating(podcastUuid: String, rate: Int): PodcastRatingResponse
    suspend fun getPodcastRating(podcastUuid: String): PodcastRatingResponse

    // Other
    suspend fun exchangeSonos(): ExchangeSonosResponse
    fun getFilters(): Single<List<Playlist>>
    suspend fun loadStats(): StatsBundle
    fun upNextSync(request: UpNextSyncRequest): Single<UpNextSyncResponse>
    suspend fun getBookmarks(): List<Bookmark>
    suspend fun sendAnonymousFeedback(subject: String, inbox: String, message: String): Response<Void>
    suspend fun sendFeedback(subject: String, inbox: String, message: String): Response<Void>

    // Referral
    suspend fun getReferralCode(): ReferralCodeResponse
    suspend fun validateReferralCode(code: String): ReferralValidationResponse
    suspend fun redeemReferralCode(code: String): ReferralRedemptionResponse
}
