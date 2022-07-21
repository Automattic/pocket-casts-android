package au.com.shiftyjelly.pocketcasts.servers.list

import retrofit2.http.GET
import retrofit2.http.Path

interface ListDownloadServer {

    @GET("{listId}.json")
    suspend fun getPodcastList(@Path("listId") listId: String): PodcastList
}
