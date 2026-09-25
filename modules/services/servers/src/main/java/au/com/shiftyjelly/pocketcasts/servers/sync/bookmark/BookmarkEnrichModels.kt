package au.com.shiftyjelly.pocketcasts.servers.sync.bookmark

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BookmarkEnrichRequest(
    @Json(name = "transcript_snippet") val transcriptSnippet: String,
)

@JsonClass(generateAdapter = true)
data class BookmarkEnrichResponse(
    @Json(name = "title") val title: String? = null,
    @Json(name = "summary") val summary: String? = null,
    @Json(name = "error") val error: String? = null,
)
