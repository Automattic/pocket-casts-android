package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PodcastEpisodesRequest(
    @field:Json(name = "uuid") val podcastUuid: String
)
