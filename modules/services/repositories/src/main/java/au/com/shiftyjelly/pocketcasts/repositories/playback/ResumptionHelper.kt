package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.hours
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.utils.minutes
import au.com.shiftyjelly.pocketcasts.utils.seconds
import au.com.shiftyjelly.pocketcasts.utils.timeIntervalSinceNow
import java.util.Date

class ResumptionHelper(val settings: Settings) {
    private var lastPauseTime: Date? = settings.getLastPauseTime()

    fun adjustedStartTimeMsFor(episode: BaseEpisode): Int {
        if (settings.getLastPausedUUID() != episode.uuid ||
            (settings.getLastPausedAt() ?: 0) != episode.playedUpToMs
        ) {
            return episode.playedUpToMs
        }

        // the pause length rewind and the interruption rewind can both apply to the same resume,
        // take the larger of the two rather than stacking them
        val rewindMs = maxOf(pauseLengthRewindMs(), interruptionRewindMs())
        if (rewindMs <= 0) {
            return episode.playedUpToMs
        }

        return (episode.playedUpToMs - rewindMs).toInt().coerceAtLeast(0)
    }

    fun paused(episode: BaseEpisode, atPlayedUpToMs: Int, dueToInterruption: Boolean = false) {
        lastPauseTime = Date()
        settings.setLastPauseTime(Date())
        settings.setLastPausedUUID(episode.uuid)
        settings.setLastPausedAt(atPlayedUpToMs)
        settings.setLastPauseWasInterruption(dueToInterruption)
    }

    private fun pauseLengthRewindMs(): Long {
        if (!settings.intelligentPlaybackResumption.value) {
            return 0
        }

        val lastPauseTime = this.lastPauseTime ?: return 0

        return when {
            lastPauseTime.timeIntervalSinceNow() > 24.hours() -> {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "More than 24 hours since this episode was paused, jumping back 30 seconds")
                30.seconds()
            }

            lastPauseTime.timeIntervalSinceNow() > 1.hours() -> {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "More than 1 hour since this episode was paused, jumping back 15 seconds")
                15.seconds()
            }

            lastPauseTime.timeIntervalSinceNow() > 5.minutes() -> {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "More than 5 minutes since this episode was paused, jumping back 10 seconds")
                10.seconds()
            }

            else -> {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Not enough time passed since this episode was last paused, no time adjustment required")
                0
            }
        }
    }

    private fun interruptionRewindMs(): Long {
        if (!FeatureFlag.isEnabled(Feature.INTERRUPTION_REWIND) || !settings.getLastPauseWasInterruption()) {
            return 0
        }

        val rewindSeconds = settings.interruptionRewindSeconds.value
        if (rewindSeconds <= 0) {
            return 0
        }

        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Playback was interrupted, jumping back $rewindSeconds seconds")
        return rewindSeconds.seconds()
    }
}
