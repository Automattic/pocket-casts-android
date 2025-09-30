package au.com.shiftyjelly.pocketcasts.servers.search

import retrofit2.http.GET
import retrofit2.http.Query

interface SearchService {
    @GET("/autocomplete/search")
    suspend fun autoCompleteSearch(
        @Query("q") query: String,
        @Query("termsLimit") terms: Int,
        @Query("podcastsLimit") podcasts: Int,
        @Query("language") language: String,
    ) : AutoCompleteResponse
}