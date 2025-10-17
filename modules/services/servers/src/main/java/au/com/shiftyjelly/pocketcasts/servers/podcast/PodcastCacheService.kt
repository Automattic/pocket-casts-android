package au.com.shiftyjelly.pocketcasts.servers.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastRatings
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeItem
import au.com.shiftyjelly.pocketcasts.servers.search.CombinedSearchRequest
import au.com.shiftyjelly.pocketcasts.servers.search.CombinedSearchResponse
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.reactivex.Single
import java.util.Date
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

typealias SuggestedFoldersResponse = Map<String, List<String>>

@JsonClass(generateAdapter = true)
data class SearchBody(@Json(name = "podcastuuid") val podcastuuid: String, @Json(name = "searchterm") val searchterm: String)

@JsonClass(generateAdapter = true)
data class SearchResultBody(@Json(name = "episodes") val episodes: List<SearchResult>)

@JsonClass(generateAdapter = true)
data class SearchResult(@Json(name = "uuid") val uuid: String)

@JsonClass(generateAdapter = true)
data class SearchEpisodesBody(@Json(name = "term") val term: String)

@JsonClass(generateAdapter = true)
data class SearchEpisodesResultBody(@Json(name = "episodes") val episodes: List<SearchEpisodeResult>)

@JsonClass(generateAdapter = true)
data class SearchEpisodeResult(
    @Json(name = "uuid") val uuid: String,
    @Json(name = "title") val title: String?,
    @Json(name = "duration") val duration: Double?,
    @Json(name = "published_date") val publishedAt: Date?,
    @Json(name = "podcast_uuid") val podcastUuid: String,
    @Json(name = "podcast_title") val podcastTitle: String?,
) {
    fun toEpisodeItem(): EpisodeItem {
        return EpisodeItem(
            uuid = uuid,
            title = title ?: "",
            duration = duration ?: 0.0,
            publishedAt = publishedAt ?: Date(),
            podcastUuid = podcastUuid,
            podcastTitle = podcastTitle ?: "",
        )
    }
}

@JsonClass(generateAdapter = true)
data class PodcastRatingsResponse(
    @Json(name = "average") val average: Double?,
    @Json(name = "total") val total: Int?,
) {
    fun toPodcastRatings(podcastUuid: String) = PodcastRatings(
        podcastUuid = podcastUuid,
        average = average ?: 0.0,
        total = total ?: 0,
    )
}

@JsonClass(generateAdapter = true)
data class SuggestedFoldersRequest(
    @Json(name = "uuids") val uuids: List<String>,
)

interface PodcastCacheService {
    @GET("/mobile/podcast/full/{podcastUuid}")
    suspend fun getPodcastAndEpisodesRaw(@Path("podcastUuid") podcastUuid: String): Response<PodcastResponse>

    @GET("/mobile/podcast/full/{podcastUuid}")
    fun getPodcastAndEpisodes(@Path("podcastUuid") podcastUuid: String): Single<PodcastResponse>

    @GET("/mobile/show_notes/full/{podcastUuid}")
    suspend fun getShowNotesLocation(@Path("podcastUuid") podcastUuid: String, @Query("disableredirect") disableRedirect: Boolean = true): ShowNotesLocationResponse

    @GET
    suspend fun getShowNotes(@Url url: String): ShowNotesResponse

    @GET("/mobile/show_notes/full/{podcastUuid}")
    @Headers("Cache-Control: only-if-cached, max-stale=7776000") // Use offline cache available for 90 days
    suspend fun getShowNotesLocationCache(@Path("podcastUuid") podcastUuid: String, @Query("disableredirect") disableRedirect: Boolean = true): ShowNotesLocationResponse

    @GET
    @Headers("Cache-Control: only-if-cached, max-stale=7776000") // Use offline cache available for 90 days
    suspend fun getShowNotesCache(@Url url: String): ShowNotesResponse

    @GET
    suspend fun getShowNotesChapters(@Url url: String): RawChaptersResponse

    @GET("/mobile/podcast/findbyepisode/{podcastUuid}/{episodeUuid}")
    fun getPodcastAndEpisodeSingle(@Path("podcastUuid") podcastUuid: String, @Path("episodeUuid") episodeUuid: String): Single<PodcastResponse>

    @GET("/mobile/podcast/findbyepisode/{podcastUuid}/{episodeUuid}")
    suspend fun getPodcastAndEpisode(@Path("podcastUuid") podcastUuid: String, @Path("episodeUuid") episodeUuid: String): PodcastResponse

    @GET("/mobile/episode/url/{podcastUuid}/{episodeUuid}")
    suspend fun getEpisodeUrl(@Path("podcastUuid") podcastUuid: String, @Path("episodeUuid") episodeUuid: String): Response<ResponseBody>

    @POST("/mobile/podcast/episode/search")
    fun searchPodcastForEpisodes(@Body searchBody: SearchBody): Single<SearchResultBody>

    @POST("/episode/search")
    fun searchEpisodes(@Body body: SearchEpisodesBody): Single<SearchEpisodesResultBody>

    @GET("/podcast/rating/{podcastUuid}")
    suspend fun getPodcastRatings(@Path("podcastUuid") podcastUuid: String): PodcastRatingsResponse

    @GET("/podcast/rating/{podcastUuid}")
    @Headers("Cache-Control: no-cache")
    suspend fun getPodcastRatingsNoCache(@Path("podcastUuid") podcastUuid: String): PodcastRatingsResponse

    @POST("/podcast/suggest_folders")
    suspend fun suggestedFolders(@Body request: SuggestedFoldersRequest): SuggestedFoldersResponse

    @POST("/search/combined")
    suspend fun combinedSearch(
        @Body request: CombinedSearchRequest,
    ): CombinedSearchResponse
}
