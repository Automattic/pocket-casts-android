package au.com.shiftyjelly.pocketcasts.servers

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class ApiTokenRequest(
    @field:Json(name = "email") val email: String,
    @field:Json(name = "password") val password: String,
    @field:Json(name = "scope") val scope: String
)
