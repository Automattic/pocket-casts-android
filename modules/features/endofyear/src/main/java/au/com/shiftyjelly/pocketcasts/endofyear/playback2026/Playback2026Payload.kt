package au.com.shiftyjelly.pocketcasts.endofyear.playback2026

import au.com.shiftyjelly.pocketcasts.repositories.endofyear.PlaybackStats
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import com.automattic.pocketcasts.playback.PlaybackPayload
import com.pocketcasts.service.api.Playback2026Archetype
import java.time.Year

/**
 * Builds the facts Playback needs from the user's listening history, and the listener
 * archetype the server works out.
 *
 * The experience computes everything derived (top shows, best month, comparison) itself, so
 * this only supplies facts.
 */
internal fun PlaybackStats.toPlaybackPayload(year: Year, archetype: PlaybackPayload.Archetype?) = PlaybackPayload(
    year = year.value,
    podcasts = podcasts.map { podcast ->
        PlaybackPayload.Podcast(
            uuid = podcast.uuid,
            title = podcast.title,
            author = podcast.author,
            artworkUrl = PodcastImage.getMediumArtworkUrl(podcast.uuid),
            playedSeconds = podcast.playedSeconds,
            episodesPlayed = podcast.playedEpisodeCount,
        )
    },
    monthlySeconds = monthlySeconds,
    previousYearSeconds = lastYearSeconds.takeIf { it > 0 },
    completion = if (startedEpisodeCount > 0) {
        PlaybackPayload.Completion(started = startedEpisodeCount, completed = completedEpisodeCount)
    } else {
        null
    },
    archetype = archetype,
)

/** The page's id for the server's archetype, or null when the server sent none it knows. */
internal fun Playback2026Archetype.toPlaybackArchetype() = when (this) {
    Playback2026Archetype.FLANEUR -> PlaybackPayload.Archetype.Flaneur
    Playback2026Archetype.AVANT_GARDIST -> PlaybackPayload.Archetype.AvantGardist
    Playback2026Archetype.COLLECTOR -> PlaybackPayload.Archetype.Collector
    Playback2026Archetype.AFICIONADO -> PlaybackPayload.Archetype.Aficionado
    Playback2026Archetype.ANTHOLOGIST -> PlaybackPayload.Archetype.Anthologist
    Playback2026Archetype.PLAYBACK_2026_ARCHETYPE_UNSPECIFIED, Playback2026Archetype.UNRECOGNIZED -> null
}
