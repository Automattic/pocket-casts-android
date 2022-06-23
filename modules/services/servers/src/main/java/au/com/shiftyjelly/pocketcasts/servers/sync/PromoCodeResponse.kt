package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
data class PromoCodeRequest(
    @field:Json(name = "code") val code: String
)

@JsonClass(generateAdapter = true)
data class PromoCodeResponse(
    @field:Json(name = "code") val code: String,
    @field:Json(name = "description") val description: String,
    @field:Json(name = "starts_at") val startsAt: Date,
    @field:Json(name = "ends_at") val endsAt: Date
)
