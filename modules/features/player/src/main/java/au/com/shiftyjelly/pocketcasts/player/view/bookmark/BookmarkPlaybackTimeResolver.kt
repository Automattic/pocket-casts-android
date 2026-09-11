package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.ChapterSeekResult
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class BookmarkPlaybackTimeResolver @Inject constructor(
    private val fingerprintTimingManager: FingerprintTimingManager,
) {
    suspend fun playbackTimeMs(
        episode: BaseEpisode,
        referenceTimeSecs: Int?,
        fallbackTimeSecs: Int,
    ): Int {
        val fallbackMs = fallbackTimeSecs * 1000
        if (referenceTimeSecs == null || !FeatureFlag.isEnabled(Feature.SYNCED_TRANSCRIPTS)) {
            return fallbackMs
        }
        return when (val result = fingerprintTimingManager.resolvePlaybackTime(episode, referenceTimeSecs.seconds)) {
            is ChapterSeekResult.Resolved -> result.playbackTime.inWholeMilliseconds.toInt()
            is ChapterSeekResult.Unresolved -> fallbackMs
        }
    }
}
