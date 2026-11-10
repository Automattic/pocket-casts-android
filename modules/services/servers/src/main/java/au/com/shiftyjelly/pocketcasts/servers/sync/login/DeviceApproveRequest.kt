package au.com.shiftyjelly.pocketcasts.servers.sync.login

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeviceApproveRequest(
    @Json(name = "user_code") val userCode: String,
    @Json(name = "deny") val deny: Boolean,
)
