package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdatePasswordRequest(
    @Json(name = "new_password") val newPassword: String,
    @Json(name = "old_password") val oldPassword: String,
    @Json(name = "scope") val scope: String,
)
