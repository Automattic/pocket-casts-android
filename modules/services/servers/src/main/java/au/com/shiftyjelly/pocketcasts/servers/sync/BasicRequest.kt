package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BasicRequest(
    @field:Json(name = "m") val model: String,
    @field:Json(name = "v") val version: Int
)
