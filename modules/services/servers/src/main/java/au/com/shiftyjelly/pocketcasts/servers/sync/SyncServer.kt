package au.com.shiftyjelly.pocketcasts.servers.sync

import au.com.shiftyjelly.pocketcasts.models.to.HistorySyncRequest
import au.com.shiftyjelly.pocketcasts.models.to.HistorySyncResponse
import io.reactivex.Single
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Url

interface SyncServer {
    @POST("/user/login")
    fun login(@Body request: LoginRequest): Single<LoginResponse>

    @POST("/user/change_email")
    fun emailChange(@Header("Authorization") authorization: String, @Body request: EmailChangeRequest): Single<UserChangeResponse>

    @POST("/user/delete_account")
    fun deleteAccount(@Header("Authorization") authorization: String): Single<UserChangeResponse>

    @POST("/user/change_password")
    fun pwdChange(@Header("Authorization") authorization: String, @Body request: PwdChangeRequest): Single<PwdChangeResponse>

    @POST("/user/named_settings/update")
    suspend fun namedSettings(@Header("Authorization") authorization: String, @Body request: NamedSettingsRequest): NamedSettingsResponse

    @POST("/up_next/sync")
    fun upNextSync(@Header("Authorization") authorization: String, @Body request: UpNextSyncRequest): Single<UpNextSyncResponse>

    @POST("/user/last_sync_at")
    fun getLastSyncAt(@Header("Authorization") authorization: String, @Body request: BasicRequest): Single<LastSyncAtResponse>

    @POST("/user/podcast/episodes")
    fun getPodcastEpisodes(@Header("Authorization") authorization: String, @Body request: PodcastEpisodesRequest): Single<PodcastEpisodesResponse>

    @POST("/user/podcast/list")
    fun getPodcastList(@Header("Authorization") authorization: String, @Body request: BasicRequest): Single<PodcastListResponse>

    @POST("/user/playlist/list")
    fun getFilterList(@Header("Authorization") authorization: String, @Body request: BasicRequest): Single<FilterListResponse>

    @POST("/history/sync")
    fun historySync(@Header("Authorization") authorization: String, @Body request: HistorySyncRequest): Single<HistorySyncResponse>

    @POST("/sync/update_episode")
    fun episodeProgressSync(@Header("Authorization") authorization: String, @Body request: EpisodeSyncRequest): Single<Void>

    @GET("/subscription/status")
    fun subscriptionStatus(@Header("Authorization") authorization: String): Single<SubscriptionStatusResponse>

    @POST("/subscription/purchase/android")
    fun subscriptionPurchase(@Header("Authorization") authorization: String, @Body request: SubscriptionPurchaseRequest): Single<SubscriptionStatusResponse>

    @GET("/files")
    fun getFiles(@Header("Authorization") authorization: String): Single<Response<FilesResponse>>

    @POST("/files")
    fun postFiles(@Header("Authorization") authorization: String, @Body body: FilePostBody): Single<Response<Void>>

    @POST("/files/upload/request")
    fun getUploadUrl(@Header("Authorization") authorization: String, @Body body: FileUploadData): Single<FileUploadResponse>

    @POST("/files/upload/image")
    fun getImageUploadUrl(@Header("Authorization") authorization: String, @Body body: FileImageUploadData): Single<FileUrlResponse>

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

    @POST("/subscription/cancel/web")
    fun cancelSubscription(@Header("Authorization") authorization: String, @Body request: SupporterCancelRequest): Single<Response<Void>>
}
