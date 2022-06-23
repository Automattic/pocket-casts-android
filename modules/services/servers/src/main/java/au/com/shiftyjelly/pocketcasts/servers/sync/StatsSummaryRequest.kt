package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StatsSummaryRequest(
    @field:Json(name = "deviceId") val deviceId: String,
    @field:Json(name = "deviceType") val deviceType: Int = 2
)
