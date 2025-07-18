package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
data class PromoCodeRequest(
    @Json(name = "code") val code: String,
)

@JsonClass(generateAdapter = true)
data class PromoCodeResponse(
    @Json(name = "code") val code: String,
    @Json(name = "description") val description: String,
    @Json(name = "starts_at") val startsAt: Date,
    @Json(name = "ends_at") val endsAt: Date,
)
