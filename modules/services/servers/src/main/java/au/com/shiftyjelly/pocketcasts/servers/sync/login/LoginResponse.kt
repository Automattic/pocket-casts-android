package au.com.shiftyjelly.pocketcasts.servers.sync.login

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @field:Json(name = "token") val token: String,
    @field:Json(name = "uuid") val uuid: String,
    @field:Json(name = "email") val email: String?
)
