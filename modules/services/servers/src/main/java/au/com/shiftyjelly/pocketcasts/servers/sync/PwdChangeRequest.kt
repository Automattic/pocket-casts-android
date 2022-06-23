package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PwdChangeRequest(
    @field:Json(name = "new_password") val email: String,
    @field:Json(name = "old_password") val password: String,
    @field:Json(name = "scope") val scope: String
)
