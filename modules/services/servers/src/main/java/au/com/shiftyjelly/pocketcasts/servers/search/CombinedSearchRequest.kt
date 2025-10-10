package au.com.shiftyjelly.pocketcasts.servers.search

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CombinedSearchRequest(
    val term: String,
)
