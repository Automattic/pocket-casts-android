package au.com.shiftyjelly.pocketcasts.servers.list

import au.com.shiftyjelly.pocketcasts.servers.refresh.StatusResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ListUploadServer {

    @POST("/share/list")
    suspend fun createPodcastList(@Body request: PodcastList): StatusResponse<ListUploadResponse>
}
