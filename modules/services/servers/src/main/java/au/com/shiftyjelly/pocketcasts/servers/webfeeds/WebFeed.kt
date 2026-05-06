package au.com.shiftyjelly.pocketcasts.servers.webfeeds

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WebFeed(
    @Json(name = "title") val title: String,
    @Json(name = "href") val href: String,
    @Json(name = "type") val type: String,
)
