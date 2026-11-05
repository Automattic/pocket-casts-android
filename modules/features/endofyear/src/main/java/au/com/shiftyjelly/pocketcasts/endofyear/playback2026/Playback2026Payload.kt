package au.com.shiftyjelly.pocketcasts.endofyear.playback2026

import au.com.shiftyjelly.pocketcasts.repositories.endofyear.PlaybackStats
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import com.automattic.pocketcasts.playback.PlaybackPayload
import java.time.Year

/**
 * Builds the facts Playback needs from the user's listening history.
 *
 * The experience computes everything derived (top shows, best month, comparison, archetype)
 * itself, so this only supplies facts.
 */
internal fun PlaybackStats.toPlaybackPayload(year: Year) = PlaybackPayload(
    year = year.value,
    podcasts = podcasts.map { podcast ->
        PlaybackPayload.Podcast(
            uuid = podcast.uuid,
            title = podcast.title,
            author = podcast.author,
            genre = podcast.firstCategory ?: "Other",
            artworkUrl = PodcastImage.getMediumArtworkUrl(podcast.uuid),
            playedSeconds = podcast.playedSeconds,
            episodesPlayed = podcast.playedEpisodeCount,
            // TODO: `listenerCount` and `earlyFollower` come from the backend (POC-1007). Until then the
            // Collector and Avant-gardist archetypes can't be earned and the simplified rules pick from the rest.
        )
    },
    monthlySeconds = monthlySeconds,
    previousYearSeconds = lastYearSeconds.takeIf { it > 0 },
    completion = if (startedEpisodeCount > 0) {
        PlaybackPayload.Completion(started = startedEpisodeCount, completed = completedEpisodeCount)
    } else {
        null
    },
)
