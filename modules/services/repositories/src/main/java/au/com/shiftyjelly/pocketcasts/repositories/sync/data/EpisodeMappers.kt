package au.com.shiftyjelly.pocketcasts.repositories.sync.data

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.servers.extensions.toDate
import com.pocketcasts.service.api.EpisodeResponse
import com.pocketcasts.service.api.publishedOrNull
import java.util.Date

internal fun EpisodeResponse.toPodcastEpisode() = PodcastEpisode(
    uuid = uuid,
    downloadUrl = url,
    publishedDate = publishedOrNull?.toDate() ?: Date(0),
    duration = duration.toDouble(),
    fileType = fileType,
    title = title,
    sizeInBytes = size,
    playingStatus = EpisodePlayingStatus.fromInt(playingStatus),
    playedUpTo = playedUpTo.toDouble(),
    isStarred = starred,
    podcastUuid = podcastUuid,
    type = episodeType,
    season = episodeSeason.toLong(),
    number = episodeNumber.toLong(),
    isArchived = isDeleted,
    slug = slug,
)

internal fun EpisodeResponse.toPodcast() = Podcast(
    uuid = podcastUuid,
    title = podcastTitle,
    author = author,
    slug = podcastSlug,
)
