package au.com.shiftyjelly.pocketcasts.servers.sync.login

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeviceTokenRequest(
    @Json(name = "grant_type") val grantType: String = "urn:ietf:params:oauth:grant-type:device_code",
    @Json(name = "device_code") val deviceCode: String,
    @Json(name = "scope") val scope: String,
)
