package au.com.shiftyjelly.pocketcasts.servers.sync

import au.com.shiftyjelly.pocketcasts.models.to.HistorySyncRequest
import au.com.shiftyjelly.pocketcasts.models.to.HistorySyncResponse
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
import com.pocketcasts.service.api.BookmarkRequest
import com.pocketcasts.service.api.BookmarksResponse
import com.pocketcasts.service.api.PodcastRatingAddRequest
import com.pocketcasts.service.api.PodcastRatingResponse
import com.pocketcasts.service.api.PodcastRatingShowRequest
import com.pocketcasts.service.api.ReferralCodeResponse
import com.pocketcasts.service.api.ReferralRedemptionRequest
import com.pocketcasts.service.api.ReferralRedemptionResponse
import com.pocketcasts.service.api.ReferralValidationResponse
import com.pocketcasts.service.api.SupportFeedbackRequest
import com.pocketcasts.service.api.UserPodcastListRequest
import com.pocketcasts.service.api.UserPodcastListResponse
import io.reactivex.Completable
import io.reactivex.Single
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface SyncService {
    @POST("/user/login_pocket_casts")
    suspend fun loginPocketCasts(@Body request: LoginPocketCastsRequest): LoginTokenResponse

    @POST("/user/login_google")
    suspend fun loginGoogle(@Body request: LoginGoogleRequest): LoginTokenResponse

    @POST("/user/token")
    suspend fun loginToken(@Body request: LoginTokenRequest): LoginTokenResponse

    @POST("/user/register_pocket_casts")
    suspend fun register(@Body request: RegisterRequest): LoginTokenResponse

    @POST("/user/forgot_password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): ForgotPasswordResponse

    @POST("/user/exchange_sonos")
    suspend fun exchangeSonos(@Header("Authorization") authorization: String): ExchangeSonosResponse

    @POST("/user/change_email")
    suspend fun emailChange(@Header("Authorization") authorization: String, @Body request: EmailChangeRequest): UserChangeResponse

    @POST("/user/delete_account")
    fun deleteAccount(@Header("Authorization") authorization: String): Single<UserChangeResponse>

    @POST("/user/update_password")
    suspend fun updatePassword(@Header("Authorization") authorization: String, @Body request: UpdatePasswordRequest): LoginTokenResponse

    @POST("/user/named_settings/update")
    suspend fun namedSettings(@Header("Authorization") authorization: String, @Body request: NamedSettingsRequest): NamedSettingsResponse

    @FormUrlEncoded
    @POST("/sync/update")
    fun syncUpdate(@FieldMap fields: Map<String, String>): Single<au.com.shiftyjelly.pocketcasts.servers.sync.update.SyncUpdateResponse>

    @POST("/up_next/sync")
    fun upNextSync(@Header("Authorization") authorization: String, @Body request: UpNextSyncRequest): Single<UpNextSyncResponse>

    @POST("/user/last_sync_at")
    fun getLastSyncAt(@Header("Authorization") authorization: String, @Body request: BasicRequest): Single<LastSyncAtResponse>

    @POST("/user/podcast/episodes")
    fun getPodcastEpisodes(@Header("Authorization") authorization: String, @Body request: PodcastEpisodesRequest): Single<PodcastEpisodesResponse>

    @Headers("Content-Type: application/octet-stream")
    @POST("/user/podcast/list")
    suspend fun getPodcastList(@Header("Authorization") authorization: String, @Body request: UserPodcastListRequest): UserPodcastListResponse

    @POST("/user/playlist/list")
    fun getFilterList(@Header("Authorization") authorization: String, @Body request: BasicRequest): Single<FilterListResponse>

    @POST("/history/sync")
    fun historySync(@Header("Authorization") authorization: String, @Body request: HistorySyncRequest): Single<HistorySyncResponse>

    @POST("/history/year")
    suspend fun historyYear(@Header("Authorization") authorization: String, @Body request: HistoryYearSyncRequest): HistoryYearResponse

    @POST("/sync/update_episode")
    fun episodeProgressSync(@Header("Authorization") authorization: String, @Body request: EpisodeSyncRequest): Completable

    @GET("/subscription/status")
    fun subscriptionStatus(@Header("Authorization") authorization: String): Single<SubscriptionStatusResponse>

    @POST("/subscription/purchase/android")
    fun subscriptionPurchase(@Header("Authorization") authorization: String, @Body request: SubscriptionPurchaseRequest): Single<SubscriptionStatusResponse>

    @GET("/files")
    fun getFiles(@Header("Authorization") authorization: String): Single<Response<FilesResponse>>

    @POST("/files")
    fun postFiles(@Header("Authorization") authorization: String, @Body body: FilePostBody): Single<Response<Void>>

    @POST("/files/upload/request")
    fun getFileUploadUrl(@Header("Authorization") authorization: String, @Body body: FileUploadData): Single<FileUploadResponse>

    @POST("/files/upload/image")
    fun getFileImageUploadUrl(@Header("Authorization") authorization: String, @Body body: FileImageUploadData): Single<FileUrlResponse>

    @PUT
    fun uploadFile(@Url url: String, @Body requestBody: RequestBody): Call<Void>

    @PUT
    fun uploadFileNoProgress(@Url url: String, @Body requestBody: RequestBody): Single<Response<Void>>

    @GET("/files/upload/status/{uuid}")
    fun getFileUploadStatus(@Header("Authorization") authorization: String, @Path("uuid") uuid: String): Single<FileUploadStatusResponse>

    @DELETE("/files/{uuid}")
    fun deleteFile(@Header("Authorization") authorization: String, @Path("uuid") uuid: String): Single<Response<Void>>

    @DELETE("/files/image/{uuid}")
    fun deleteImageFile(@Header("Authorization") authorization: String, @Path("uuid") uuid: String): Single<Response<Void>>

    @GET("/files/{uuid}")
    fun getFile(@Header("Authorization") authorization: String, @Path("uuid") uuid: String): Single<Response<ServerFile>>

    @GET("/files/play/{uuid}")
    fun getPlaybackUrl(@Header("Authorization") authorization: String, @Path("uuid") uuid: String): Single<FileUrlResponse>

    @POST("/user/stats/summary")
    suspend fun loadStats(@Header("Authorization") authorization: String, @Body request: StatsSummaryRequest): Map<String, Any>

    @GET("/files/usage")
    fun getFilesUsage(@Header("Authorization") authorization: String): Single<FileAccount>

    @POST("/subscription/promo/redeem")
    fun redeemPromoCode(@Header("Authorization") authorization: String, @Body request: PromoCodeRequest): Single<PromoCodeResponse>

    @POST("/subscription/promo/validate")
    fun validatePromoCode(@Body request: PromoCodeRequest): Single<PromoCodeResponse>

    @Headers("Content-Type: application/octet-stream")
    @POST("/user/bookmark/list")
    suspend fun getBookmarkList(@Header("Authorization") authorization: String, @Body request: BookmarkRequest): BookmarksResponse

    @Headers("Content-Type: application/octet-stream")
    @POST("/user/podcast_rating/add")
    suspend fun addPodcastRating(@Header("Authorization") authorization: String, @Body request: PodcastRatingAddRequest): PodcastRatingResponse

    @Headers("Content-Type: application/octet-stream")
    @POST("/user/podcast_rating/show")
    suspend fun getPodcastRating(@Header("Authorization") authorization: String, @Body request: PodcastRatingShowRequest): PodcastRatingResponse

    @Headers("Content-Type: application/octet-stream")
    @POST("/anonymous/feedback")
    suspend fun sendAnonymousFeedback(@Body request: SupportFeedbackRequest): Response<Void>

    @Headers("Content-Type: application/octet-stream")
    @POST("/support/feedback")
    suspend fun sendFeedback(@Header("Authorization") authorization: String, @Body request: SupportFeedbackRequest): Response<Void>

    // Referral
    @Headers("Content-Type: application/octet-stream")
    @GET("/referrals/code")
    suspend fun getReferralCode(@Header("Authorization") authorization: String): ReferralCodeResponse

    @Headers("Content-Type: application/octet-stream")
    @GET("/referrals/validate")
    suspend fun validateReferralCode(@Header("Authorization") authorization: String, @Query("code") code: String): ReferralValidationResponse

    @Headers("Content-Type: application/octet-stream")
    @POST("/referrals/redeem")
    suspend fun redeemReferralCode(@Header("Authorization") authorization: String, @Body request: ReferralRedemptionRequest): ReferralRedemptionResponse
}
