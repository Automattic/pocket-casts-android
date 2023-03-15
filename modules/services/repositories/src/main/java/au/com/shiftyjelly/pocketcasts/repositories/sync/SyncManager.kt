package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.accounts.Account
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
import au.com.shiftyjelly.pocketcasts.servers.sync.NamedSettingsCaller
import au.com.shiftyjelly.pocketcasts.servers.sync.PodcastEpisodesResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.PodcastListResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.PromoCodeResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.ServerFile
import au.com.shiftyjelly.pocketcasts.servers.sync.SubscriptionPurchaseRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.SubscriptionStatusResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.UpNextSyncRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.UpNextSyncResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.UserChangeResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.history.HistoryYearResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.login.ExchangeSonosResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.login.LoginTokenResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.update.SyncUpdateResponse
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import retrofit2.Response
import java.io.File

interface SyncManager : NamedSettingsCaller {

    // Account
    val isLoggedInObservable: BehaviorRelay<Boolean>
    fun getUuid(): String?
    fun isLoggedIn(): Boolean
    fun isGoogleLogin(): Boolean
    fun getEmail(): String?
    fun setEmail(email: String)
    fun signOut(action: () -> Unit = {})
    fun updateEmail(email: String)
    suspend fun loginWithGoogle(idToken: String, signInSource: SignInSource): LoginResult
    suspend fun loginWithEmailAndPassword(email: String, password: String, signInSource: SignInSource): LoginResult
    suspend fun createUserWithEmailAndPassword(email: String, password: String): LoginResult
    suspend fun forgotPassword(email: String, onSuccess: () -> Unit, onError: (String) -> Unit)
    fun setRefreshToken(refreshToken: RefreshToken)
    fun setAccessToken(accessToken: AccessToken)
    fun peekAccessToken(account: Account): AccessToken?
    suspend fun refreshAccessToken(account: Account): AccessToken?
    fun emailChange(newEmail: String, password: String): Single<UserChangeResponse>
    fun deleteAccount(): Single<UserChangeResponse>
    suspend fun updatePassword(newPassword: String, oldPassword: String): LoginTokenResponse

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
    fun getHomeFolder(): Single<PodcastListResponse>
    fun getPodcastEpisodes(podcastUuid: String): Single<PodcastEpisodesResponse>
    fun syncUpdate(data: String, lastModified: String): Single<SyncUpdateResponse>
    fun episodeSync(request: EpisodeSyncRequest): Completable

    // Other
    suspend fun exchangeSonos(): ExchangeSonosResponse
    fun getFilters(): Single<List<Playlist>>
    suspend fun loadStats(): StatsBundle
    fun upNextSync(request: UpNextSyncRequest): Single<UpNextSyncResponse>
}
