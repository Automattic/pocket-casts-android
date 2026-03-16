package au.com.shiftyjelly.pocketcasts.servers.analytics

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface AnalyticsLiveService {
    @POST
    suspend fun sendEvents(@Url url: String, @Body events: List<InputEvent>): Response<Unit>
}
