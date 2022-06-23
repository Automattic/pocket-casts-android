package au.com.shiftyjelly.pocketcasts.servers.refresh

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface RefreshServer {

    @POST("import/opml") suspend fun importOpml(@Body request: ImportOpmlRequest): Response<StatusResponse<ImportOpmlResponse>>

    @POST("podcasts/refresh") suspend fun refreshPodcastFeed(@Body request: RefreshPodcastFeedRequest): Response<StatusResponse<BasicResponse>>
}
