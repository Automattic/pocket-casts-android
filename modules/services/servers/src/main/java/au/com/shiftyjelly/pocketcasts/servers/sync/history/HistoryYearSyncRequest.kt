package au.com.shiftyjelly.pocketcasts.servers.sync.history

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HistoryYearSyncRequest(
    @Json(name = "version") val version: String = "1",
    @Json(name = "count") val count: Boolean,
    @Json(name = "year") val year: Int,
)
