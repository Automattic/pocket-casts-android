package au.com.shiftyjelly.pocketcasts.servers.refresh

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BasicResponse(
    @field:Json(name = "status") val status: String,
    @field:Json(name = "message") val message: String
)
