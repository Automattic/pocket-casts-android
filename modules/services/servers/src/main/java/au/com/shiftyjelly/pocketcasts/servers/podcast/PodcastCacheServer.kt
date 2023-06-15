package au.com.shiftyjelly.pocketcasts.servers.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastRatings
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeItem
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.Date

@JsonClass(generateAdapter = true)
data class SearchBody(@field:Json(name = "podcastuuid") val podcastuuid: String, @field:Json(name = "searchterm") val searchterm: String)

@JsonClass(generateAdapter = true)
data class SearchResultBody(@field:Json(name = "episodes") val episodes: List<SearchResult>)

@JsonClass(generateAdapter = true)
data class SearchResult(@field:Json(name = "uuid") val uuid: String)

@JsonClass(generateAdapter = true)
data class SearchEpisodesBody(@field:Json(name = "term") val term: String)

@JsonClass(generateAdapter = true)
data class SearchEpisodesResultBody(@field:Json(name = "episodes") val episodes: List<SearchEpisodeResult>)

@JsonClass(generateAdapter = true)
data class SearchEpisodeResult(
    @field:Json(name = "uuid") val uuid: String,
    @field:Json(name = "title") val title: String?,
    @field:Json(name = "duration") val duration: Double?,
    @field:Json(name = "published_date") val publishedAt: Date?,
    @field:Json(name = "podcast_uuid") val podcastUuid: String,
    @field:Json(name = "podcast_title") val podcastTitle: String?,
) {
    fun toEpisodeItem(): EpisodeItem {
        return EpisodeItem(
            uuid = uuid,
            title = title ?: "",
            duration = duration ?: 0.0,
            publishedAt = publishedAt ?: Date(),
            podcastUuid = podcastUuid,
            podcastTitle = podcastTitle ?: ""
        )
    }
}

@JsonClass(generateAdapter = true)
data class PodcastRatingsResponse(
    @field:Json(name = "average") val average: Double?,
    @field:Json(name = "total") val total: Int?,
) {
    fun toPodcastRatings(podcastUuid: String) = PodcastRatings(
        podcastUuid = podcastUuid,
        average = average ?: 0.0,
        total = total
    )
}

interface PodcastCacheServer {
    @GET("/mobile/podcast/full/{podcastUuid}")
    fun getPodcastAndEpisodesRaw(@Path("podcastUuid") podcastUuid: String): Single<Response<PodcastResponse>>

    @GET("/mobile/podcast/full/{podcastUuid}")
    fun getPodcastAndEpisodes(@Path("podcastUuid") podcastUuid: String): Single<PodcastResponse>

    @GET("/mobile/show_notes/full/{podcastUuid}")
    suspend fun getShowNotes(@Path("podcastUuid") podcastUuid: String): ShowNotesResponse

    @GET("/mobile/show_notes/full/{podcastUuid}")
    @Headers("Cache-Control: only-if-cached, max-stale=7776000") // Use offline cache available for 90 days
    suspend fun getShowNotesCache(@Path("podcastUuid") podcastUuid: String): ShowNotesResponse

    @GET("/mobile/podcast/findbyepisode/{podcastUuid}/{episodeUuid}")
    fun getPodcastAndEpisode(@Path("podcastUuid") podcastUuid: String, @Path("episodeUuid") episodeUuid: String): Single<PodcastResponse>

    @POST("/mobile/podcast/episode/search")
    fun searchPodcastForEpisodes(@Body searchBody: SearchBody): Single<SearchResultBody>

    @POST("/episode/search")
    fun searchEpisodes(@Body body: SearchEpisodesBody): Single<SearchEpisodesResultBody>

    @GET("/podcast/rating/{podcastUuid}")
    suspend fun getPodcastRatings(@Path("podcastUuid") podcastUuid: String): PodcastRatingsResponse
}
