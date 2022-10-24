package au.com.shiftyjelly.pocketcasts.servers.bumpstats

import retrofit2.http.Body
import retrofit2.http.POST

interface WpComServer {
    /**
     * Notify the that an event X occurred at Y time.
     */
    @POST("/rest/v1.1/tracks/record")
    suspend fun bumpStatAnonymously(@Body request: AnonymousBumpStatsRequest)
}
