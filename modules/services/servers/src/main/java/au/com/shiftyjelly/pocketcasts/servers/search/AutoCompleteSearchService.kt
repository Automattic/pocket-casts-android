package au.com.shiftyjelly.pocketcasts.servers.search

import retrofit2.http.GET
import retrofit2.http.Query

interface AutoCompleteSearchService {
    @GET("/autocomplete/search")
    suspend fun autoCompleteSearch(
        @Query("q") query: String,
        @Query("termsLimit") termsLimit: Int?,
        @Query("podcastsLimit") podcastsLimit: Int?,
    ): AutoCompleteResponse
}
