package au.com.shiftyjelly.pocketcasts.servers.refresh

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StatusResponse<T>(
    @field:Json(name = "status") var status: String? = null,
    @field:Json(name = "message") var message: String? = null,
    @field:Json(name = "result") var result: T? = null
)
