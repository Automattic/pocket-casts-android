package au.com.shiftyjelly.pocketcasts.servers.refresh

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ImportOpmlResponse(
    @Json(name = "uuids") val uuids: List<String>,
    @Json(name = "poll_uuids") val pollUuids: List<String>,
    @Json(name = "failed") var failed: Int,
)
