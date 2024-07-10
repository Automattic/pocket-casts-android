package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PodcastRatingAddRequest(
    @field:Json(name = "podcastUuid") val podcastUuid: String,
    @field:Json(name = "podcastRating") val podcastRating: Int,
)

@JsonClass(generateAdapter = true)
data class PodcastRatingResponse(
    @field:Json(name = "podcastUuid") val podcastUuid: String,
    @field:Json(name = "modifiedAt") val modifiedAt: String,
    @field:Json(name = "podcastRating") val podcastRating: Int,
)

@JsonClass(generateAdapter = true)
data class PodcastRatingShowRequest(
    @field:Json(name = "podcastUuid") val podcastUuid: String,
)
