package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.hours
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.utils.minutes
import au.com.shiftyjelly.pocketcasts.utils.seconds
import au.com.shiftyjelly.pocketcasts.utils.timeIntervalSinceNow
import java.util.Date

class ResumptionHelper(val settings: Settings) {
    private var lastPauseTime: Date? = settings.getLastPauseTime()

    fun adjustedStartTimeMsFor(episode: BaseEpisode): Int {
        if (!settings.intelligentPlaybackResumption.value ||
            settings.getLastPausedUUID() != episode.uuid ||
            (settings.getLastPausedAt() ?: 0) != episode.playedUpToMs
        ) return episode.playedUpToMs

        val lastPauseTime = this.lastPauseTime ?: return episode.playedUpToMs

        val adjustedTime = when {
            lastPauseTime.timeIntervalSinceNow() > 24.hours() -> {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "More than 24 hours since this episode was paused, jumping back 30 seconds")
                episode.playedUpToMs - 30.seconds()
            }
            lastPauseTime.timeIntervalSinceNow() > 1.hours() -> {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "More than 1 hour since this episode was paused, jumping back 15 seconds")
                episode.playedUpToMs - 15.seconds()
            }
            lastPauseTime.timeIntervalSinceNow() > 5.minutes() -> {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "More than 5 minutes since this episode was paused, jumping back 10 seconds")
                episode.playedUpToMs - 10.seconds()
            }
            else -> {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Not enough time passed since this episode was last paused, no time adjustment required")
                episode.playedUpToMs.toLong()
            }
        }

        return adjustedTime.toInt().coerceAtLeast(0)
    }

    fun paused(episode: BaseEpisode, atPlayedUpToMs: Int) {
        lastPauseTime = Date()
        settings.setLastPauseTime(Date())
        settings.setLastPausedUUID(episode.uuid)
        settings.setLastPausedAt(atPlayedUpToMs)
    }
}
