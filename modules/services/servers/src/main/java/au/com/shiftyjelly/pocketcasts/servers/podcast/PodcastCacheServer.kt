package au.com.shiftyjelly.pocketcasts.servers.podcast

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

@JsonClass(generateAdapter = true)
data class SearchBody(@field:Json(name = "podcastuuid") val podcastuuid: String, @field:Json(name = "searchterm") val searchterm: String)

@JsonClass(generateAdapter = true)
data class SearchResultBody(@field:Json(name = "episodes") val episodes: List<SearchResult>)

@JsonClass(generateAdapter = true)
data class SearchResult(@field:Json(name = "uuid") val uuid: String)

interface PodcastCacheServer {
    @GET("/mobile/podcast/full/{podcastUuid}/{pageNumber}/{sortOption}/{episodeLimit}")
    fun getPodcastAndEpisodesRaw(@Path("podcastUuid") podcastUuid: String, @Path("pageNumber") pageNumber: Int = 0, @Path("sortOption") sortOption: Int = 3, @Path("episodeLimit") episodeLimit: Int = 0): Single<Response<PodcastResponse>>

    @GET("/mobile/podcast/full/{podcastUuid}/{pageNumber}/{sortOption}/{episodeLimit}")
    fun getPodcastAndEpisodes(@Path("podcastUuid") podcastUuid: String, @Path("pageNumber") pageNumber: Int = 0, @Path("sortOption") sortOption: Int = 3, @Path("episodeLimit") episodeLimit: Int = 0): Single<PodcastResponse>

    @GET("/mobile/podcast/findbyepisode/{podcastUuid}/{episodeUuid}")
    fun getPodcastAndEpisode(@Path("podcastUuid") podcastUuid: String, @Path("episodeUuid") episodeUuid: String): Single<PodcastResponse>

    @POST("/mobile/podcast/episode/search")
    fun searchPodcastForEpisodes(@Body searchBody: SearchBody): Single<SearchResultBody>
}
