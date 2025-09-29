package au.com.shiftyjelly.pocketcasts.servers.search

import retrofit2.http.POST
import retrofit2.http.Query

interface SearchService {
    @POST("/autocomplete/search")
    suspend fun autocompleteSearch(
        @Query("q") query: String,
        @Query("termsLimit") limit: Int,
        @Query("podcastsLimit") items: Int,
        @Query("language") language: String,
    ) : AutoCompleteResponse
}