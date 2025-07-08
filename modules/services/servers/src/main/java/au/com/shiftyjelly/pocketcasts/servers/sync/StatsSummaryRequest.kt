package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StatsSummaryRequest(
    @Json(name = "deviceId") val deviceId: String,
    @Json(name = "deviceType") val deviceType: Int = 2,
)
