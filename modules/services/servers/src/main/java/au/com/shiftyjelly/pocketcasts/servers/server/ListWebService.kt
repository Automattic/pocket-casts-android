package au.com.shiftyjelly.pocketcasts.servers.server

import au.com.shiftyjelly.pocketcasts.servers.model.Discover
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

interface ListWebService {
    @GET("/discover/{platform}/content.json")
    fun getDiscoverFeed(@Path("platform") platform: String): Single<Discover>

    @GET("/discover/{platform}/content.json")
    suspend fun getDiscoverFeedSuspend(@Path("platform") platform: String): Discover

    @GET
    fun getListFeed(@Url url: String): Single<ListFeed>

    @GET
    suspend fun getListFeedSuspend(@Url url: String): ListFeed

    @GET
    fun getCategoriesList(@Url url: String): Single<List<DiscoverCategory>>
}
