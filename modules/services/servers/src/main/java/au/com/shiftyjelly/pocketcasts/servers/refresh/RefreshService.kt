package au.com.shiftyjelly.pocketcasts.servers.refresh

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface RefreshService {

    @POST("import/opml")
    suspend fun importOpml(@Body request: ImportOpmlRequest): Response<StatusResponse<ImportOpmlResponse>>

    @GET("api/v1/update_podcast")
    suspend fun updatePodcast(@Query("podcast_uuid") podcastUuid: String, @Query("last_episode_uuid") lastEpisodeUuid: String?): Response<Unit>

    @GET
    suspend fun pollUpdatePodcast(@Url url: String): Response<Unit>
}
