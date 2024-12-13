package au.com.shiftyjelly.pocketcasts.repositories.playback

import kotlin.time.Duration

data class SleepTimerState(
    val isSleepTimerRunning: Boolean = false,
    val timeLeft: Duration = Duration.ZERO,
    val numberOfEpisodesLeft: Int = 0,
    val numberOfChaptersLeft: Int = 0,
) {
    val isSleepEndOfEpisodeRunning: Boolean
        get() = numberOfEpisodesLeft != 0

    val isSleepEndOfChapterRunning: Boolean
        get() = numberOfChaptersLeft != 0
}
