package au.com.shiftyjelly.pocketcasts.servers.sync.login

import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.preferences.RefreshToken
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeviceTokenResponse(
    @Json(name = "accessToken") val accessToken: AccessToken,
    @Json(name = "refreshToken") val refreshToken: RefreshToken,
    @Json(name = "tokenType") val tokenType: String = "Bearer",
    @Json(name = "expiresIn") val expiresIn: Int = 0,
    @Json(name = "email") val email: String? = null,
    @Json(name = "uuid") val uuid: String? = null,
)
