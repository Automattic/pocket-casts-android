package au.com.shiftyjelly.pocketcasts.servers.cdn

import io.reactivex.Maybe
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Path

interface StaticServer {

    @GET("/discover/images/metadata/{podcastUuid}.json")
    fun getColorsMaybe(@Path("podcastUuid") podcastUuid: String): Maybe<ColorsResponse>

    @GET("/discover/images/metadata/{podcastUuid}.json")
    suspend fun getColors(@Path("podcastUuid") podcastUuid: String): ColorsResponse?

    @GET("/discover/json/featured.json")
    fun getFeaturedPodcasts(): Observable<PodcastsResponse>
}
