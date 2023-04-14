package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdatePasswordRequest(
    @field:Json(name = "new_password") val newPassword: String,
    @field:Json(name = "old_password") val oldPassword: String,
    @field:Json(name = "scope") val scope: String
)
