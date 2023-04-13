package au.com.shiftyjelly.pocketcasts.servers.sync.login

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginGoogleRequest(
    @field:Json(name = "id_token") val idToken: String,
    @field:Json(name = "scope") val scope: String
)
