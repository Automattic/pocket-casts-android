package au.com.shiftyjelly.pocketcasts.servers.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PodcastEpisodesResponse(
    @Json(name = "episodesSortOrder") val episodesSortOrder: Int?,
    @Json(name = "autoStartFrom") val autoStartFrom: Int?,
    @Json(name = "subscribed") val subscribed: Boolean?,
    @Json(name = "episodes") val episodes: List<PodcastEpisode>?,
)

@JsonClass(generateAdapter = true)
data class PodcastEpisode(
    @Json(name = "uuid") val uuid: String,
    @Json(name = "duration") val duration: Long?,
    @Json(name = "playingStatus") val playingStatus: Int?,
    @Json(name = "playedUpTo") val playedUpTo: Int?,
    @Json(name = "isDeleted") val isArchived: Boolean?,
    @Json(name = "starred") val starred: Boolean?,
    @Json(name = "deselectedChapters") val deselectedChapters: String?,
)
