package au.com.shiftyjelly.pocketcasts.servers.search

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SearchService {
    @GET("/autocomplete/search")
    suspend fun autoCompleteSearch(
        @Query("q") query: String,
        @Query("termsLimit") termsLimit: Int,
        @Query("podcastsLimit") podcastsLimit: Int,
        @Query("language") language: String,
    ): AutoCompleteResponse

    @POST("/search/combined")
    suspend fun combinedSearch(
        @Body request: CombinedSearchRequest,
    ): CombinedSearchResponse
}
