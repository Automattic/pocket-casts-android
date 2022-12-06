package au.com.shiftyjelly.pocketcasts.servers.sync.history

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HistoryYearSyncRequest(
    @field:Json(name = "version") val version: String = "1",
    @field:Json(name = "count") val count: Boolean,
    @field:Json(name = "year") val year: Int,
)
