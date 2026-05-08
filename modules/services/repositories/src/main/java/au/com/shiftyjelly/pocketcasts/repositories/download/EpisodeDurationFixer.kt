package au.com.shiftyjelly.pocketcasts.repositories.download

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import java.io.File
import javax.inject.Inject

/**
 * Backfills `episode.duration` after a successful download when the RSS feed
 * did not provide a usable duration. No-op when the stored duration is already
 * populated, so it never overwrites a value supplied by the feed.
 */
class EpisodeDurationFixer @Inject constructor(
    private val episodeManager: EpisodeManager,
    private val mediaDurationExtractor: MediaDurationExtractor,
) {
    fun fixMissingDuration(episode: BaseEpisode, file: File) {
        if (episode.duration > 0) {
            return
        }
        val durationInSecs = mediaDurationExtractor.extractDurationInSeconds(file) ?: return
        episodeManager.updateDurationBlocking(episode, durationInSecs, syncChanges = true)
    }
}
