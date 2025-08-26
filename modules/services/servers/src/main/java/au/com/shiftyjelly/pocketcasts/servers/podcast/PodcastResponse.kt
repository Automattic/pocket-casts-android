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
    @Json(name = "episode_frequency") val episodeFrequency: String,
    @Json(name = "estimated_next_episode_at") val estimatedNextEpisodeAt: String?,
    @Json(name = "episode_count") val episodeCount: Int,
    @Json(name = "has_more_episodes") val hasMoreEpisodes: Boolean,
    @Json(name = "has_seasons") val hasSeasons: Boolean,
    @Json(name = "season_count") val seasonCount: Int,
    @Json(name = "refresh_allowed") val refreshAllowed: Boolean?,
    @Json(name = "podcast") val podcastInfo: PodcastInfo,
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
    @Json(name = "uuid") val uuid: String,
    @Json(name = "url") val url: String?,
    @Json(name = "title") val title: String?,
    @Json(name = "author") val author: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "description_html") val descriptionHtml: String?,
    @Json(name = "show_type") val showType: String?,
    @Json(name = "category") val category: String?,
    @Json(name = "audio") val audio: Boolean?,
    @Json(name = "episodes") val episodes: List<EpisodeInfo>?,
    @Json(name = "is_private") val isPrivate: Boolean?,
    @Json(name = "fundings") val fundings: List<Funding>?,
    @Json(name = "slug") val slug: String?,
) {

    fun toPodcast(): Podcast {
        val podcast = Podcast()
        podcast.uuid = uuid
        podcast.podcastUrl = url
        podcast.title = title.orEmpty()
        podcast.author = author.orEmpty()
        podcast.podcastCategory = category.orEmpty()
        podcast.podcastHtmlDescription = descriptionHtml.orEmpty()
        podcast.podcastDescription = description.orEmpty()
        podcast.episodesSortType = if (showType == "serial") EpisodesSortType.EPISODES_SORT_BY_DATE_ASC else EpisodesSortType.EPISODES_SORT_BY_DATE_DESC
        episodes?.mapNotNull { it.toEpisode(uuid) }?.let { episodes ->
            podcast.episodes.addAll(episodes)
        }
        podcast.isPrivate = isPrivate ?: false
        podcast.fundingUrl = fundings?.firstOrNull()?.url
        podcast.slug = slug.orEmpty()
        return podcast
    }
}

@JsonClass(generateAdapter = true)
data class Funding(
    @Json(name = "url") val url: String,
    @Json(name = "title") val title: String?,
)

@JsonClass(generateAdapter = true)
data class EpisodeInfo(
    @Json(name = "uuid") val uuid: String,
    @Json(name = "title") val title: String?,
    @Json(name = "season") val season: Long?,
    @Json(name = "number") val number: Long?,
    @Json(name = "type") val type: String?,
    @Json(name = "url") val url: String,
    @Json(name = "file_type") val fileType: String?,
    @Json(name = "file_size") val fileSize: Long?,
    @Json(name = "duration") val duration: Double?,
    @Json(name = "published") val published: String,
    @Json(name = "slug") val slug: String?,
) {

    fun toEpisode(podcastUuid: String): PodcastEpisode? {
        val publishedDate = published.parseIsoDate() ?: return null
        val episodeTitle = title.orEmpty()
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
            type = type,
            slug = slug.orEmpty(),
        )
    }
}
