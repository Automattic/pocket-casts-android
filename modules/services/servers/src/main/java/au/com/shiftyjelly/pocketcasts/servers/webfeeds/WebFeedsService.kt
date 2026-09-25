package au.com.shiftyjelly.pocketcasts.servers.webfeeds

import retrofit2.http.GET
import retrofit2.http.Query

interface WebFeedsService {
    @GET("/feeds")
    suspend fun getFeeds(@Query("url") url: String): List<WebFeed>
}
