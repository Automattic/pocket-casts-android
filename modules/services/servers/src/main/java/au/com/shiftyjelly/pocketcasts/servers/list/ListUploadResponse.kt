package au.com.shiftyjelly.pocketcasts.servers.list

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ListUploadResponse(
    @field:Json(name = "share_url") var shareUrl: String
)
