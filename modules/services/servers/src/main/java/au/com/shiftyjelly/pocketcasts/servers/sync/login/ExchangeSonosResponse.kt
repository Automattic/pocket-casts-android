package au.com.shiftyjelly.pocketcasts.servers.sync.login

import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.preferences.RefreshToken
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExchangeSonosResponse(
    @Json(name = "accessToken") val accessToken: AccessToken,
    @Json(name = "refreshToken") val refreshToken: RefreshToken,
)
