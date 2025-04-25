package au.com.shiftyjelly.pocketcasts.servers.server

import au.com.shiftyjelly.pocketcasts.servers.model.Discover
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Url

interface ListWebService {
    @GET("/discover/{platform}/content_v{version}.json")
    suspend fun getDiscoverFeed(@Path("platform") platform: String, @Path("version") version: Int): Discover

    @GET
    suspend fun getListFeed(@Url url: String): ListFeed

    @GET
    suspend fun getListFeedAuthenticated(@Url url: String, @Header("Authorization") authorization: String): ListFeed

    @GET
    suspend fun getCategoriesList(@Url url: String): List<DiscoverCategory>
}
