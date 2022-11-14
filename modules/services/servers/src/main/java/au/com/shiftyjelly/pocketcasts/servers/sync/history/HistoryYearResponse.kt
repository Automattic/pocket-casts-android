package au.com.shiftyjelly.pocketcasts.servers.sync.history

import au.com.shiftyjelly.pocketcasts.models.to.HistorySyncResponse
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HistoryYearResponse(
    @field:Json(name = "count") val count: Long?,
    @field:Json(name = "history") val history: HistorySyncResponse?
)
