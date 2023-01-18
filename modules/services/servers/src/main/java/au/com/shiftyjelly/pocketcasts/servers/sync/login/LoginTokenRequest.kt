package au.com.shiftyjelly.pocketcasts.servers.sync.login

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginTokenRequest(
    @field:Json(name = "grant_type") val grantType: String = "refresh_token",
    @field:Json(name = "refresh_token") val refreshToken: String
)
