package au.com.shiftyjelly.pocketcasts.servers.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.utils.extensions.parseIsoDate
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
data class PodcastResponse(
    @field:Json(name = "episode_frequency") val episodeFrequency: String,
    @field:Json(name = "estimated_next_episode_at") val estimatedNextEpisodeAt: String?,
    @field:Json(name = "episode_count") val episodeCount: Int,
    @field:Json(name = "has_more_episodes") val hasMoreEpisodes: Boolean,
    @field:Json(name = "has_seasons") val hasSeasons: Boolean,
    @field:Json(name = "season_count") val seasonCount: Int,
    @field:Json(name = "refresh_allowed") val refreshAllowed: Boolean?,
    @field:Json(name = "podcast") val podcastInfo: PodcastInfo
) {

    // for the moshi code generation to attach a helper
    companion object

    fun toPodcast(): Podcast {
        val podcast = podcastInfo.toPodcast()
        podcast.estimatedNextEpisode = estimatedNextEpisodeAt?.parseIsoDate()
        podcast.episodeFrequency = episodeFrequency
        podcast.refreshAvailable = refreshAllowed ?: false
        return podcast
    }
}

@JsonClass(generateAdapter = true)
data class PodcastInfo(
    @field:Json(name = "uuid") val uuid: String,
    @field:Json(name = "url") val url: String?,
    @field:Json(name = "title") val title: String?,
    @field:Json(name = "author") val author: String?,
    @field:Json(name = "description") val description: String?,
    @field:Json(name = "show_type") val showType: String?,
    @field:Json(name = "category") val category: String?,
    @field:Json(name = "audio") val audio: Boolean?,
    @field:Json(name = "episodes") val episodes: List<EpisodeInfo>?,
) {

    fun toPodcast(): Podcast {
        val podcast = Podcast()
        podcast.uuid = uuid
        podcast.podcastUrl = url
        podcast.title = title ?: ""
        podcast.author = author ?: ""
        podcast.podcastCategory = category ?: ""
        podcast.podcastDescription = description ?: ""
        podcast.episodesSortType = if (showType == "serial") EpisodesSortType.EPISODES_SORT_BY_DATE_ASC else EpisodesSortType.EPISODES_SORT_BY_DATE_DESC
        episodes?.mapNotNull { it.toEpisode(uuid) }?.let { episodes ->
            podcast.episodes.addAll(episodes)
        }
        return podcast
    }
}

@JsonClass(generateAdapter = true)
data class EpisodeInfo(
    @field:Json(name = "uuid") val uuid: String,
    @field:Json(name = "title") val title: String?,
    @field:Json(name = "season") val season: Long?,
    @field:Json(name = "number") val number: Long?,
    @field:Json(name = "type") val type: String?,
    @field:Json(name = "url") val url: String,
    @field:Json(name = "file_type") val fileType: String?,
    @field:Json(name = "file_size") val fileSize: Long?,
    @field:Json(name = "duration") val duration: Double?,
    @field:Json(name = "published") val published: String
) {

    fun toEpisode(podcastUuid: String): PodcastEpisode? {
        val publishedDate = published.parseIsoDate() ?: return null
        val episodeTitle = title ?: ""
        return PodcastEpisode(
            uuid = uuid,
            downloadUrl = url,
            title = episodeTitle,
            fileType = fileType,
            sizeInBytes = fileSize ?: 0,
            duration = duration ?: 0.0,
            publishedDate = publishedDate,
            podcastUuid = podcastUuid,
            addedDate = Date(),
            season = season,
            number = number,
            type = type
        )
    }
}
