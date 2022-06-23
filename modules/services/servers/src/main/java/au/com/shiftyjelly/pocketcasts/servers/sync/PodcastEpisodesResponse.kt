package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PodcastEpisodesResponse(
    @field:Json(name = "episodesSortOrder") val episodesSortOrder: Int?,
    @field:Json(name = "autoStartFrom") val autoStartFrom: Int?,
    @field:Json(name = "subscribed") val subscribed: Boolean?,
    @field:Json(name = "episodes") val episodes: List<PodcastEpisode>?
)

@JsonClass(generateAdapter = true)
data class PodcastEpisode(
    @field:Json(name = "uuid") val uuid: String,
    @field:Json(name = "duration") val duration: Long?,
    @field:Json(name = "playingStatus") val playingStatus: Int?,
    @field:Json(name = "playedUpTo") val playedUpTo: Int?,
    @field:Json(name = "isDeleted") val isArchived: Boolean?,
    @field:Json(name = "starred") val starred: Boolean?
)
