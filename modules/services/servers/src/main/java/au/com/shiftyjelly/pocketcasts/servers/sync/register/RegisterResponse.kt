package au.com.shiftyjelly.pocketcasts.servers.sync.register

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterResponse(
    @field:Json(name = "token") val token: String,
    @field:Json(name = "uuid") val uuid: String
)
