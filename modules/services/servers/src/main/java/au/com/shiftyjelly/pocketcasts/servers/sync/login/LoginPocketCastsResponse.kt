package au.com.shiftyjelly.pocketcasts.servers.sync.login

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class LoginPocketCastsResponse(
    @field:Json(name = "email") val email: String,
    @field:Json(name = "uuid") val uuid: String,
    @field:Json(name = "isNew") val isNew: Boolean,
    @field:Json(name = "accessToken") val accessToken: String,
    @field:Json(name = "tokenType") val tokenType: String,
    @field:Json(name = "expiresIn") val expiresIn: Int,
    @field:Json(name = "refreshToken") val refreshToken: String,
)
