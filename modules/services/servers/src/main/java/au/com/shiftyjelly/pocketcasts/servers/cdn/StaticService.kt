package au.com.shiftyjelly.pocketcasts.servers.cdn

import retrofit2.http.GET
import retrofit2.http.Path

interface StaticService {

    @GET("/discover/images/metadata/{podcastUuid}.json")
    suspend fun getColors(@Path("podcastUuid") podcastUuid: String): ColorsResponse?

    @GET("/discover/blaze/promotions.json")
    suspend fun getBlazePromotions(): BlazePromotionsResponse
}
