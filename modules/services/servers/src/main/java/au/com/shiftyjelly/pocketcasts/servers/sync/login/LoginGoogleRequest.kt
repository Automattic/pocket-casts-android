package au.com.shiftyjelly.pocketcasts.servers.sync.login

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginGoogleRequest(
    @Json(name = "id_token") val idToken: String,
    @Json(name = "scope") val scope: String,
)
