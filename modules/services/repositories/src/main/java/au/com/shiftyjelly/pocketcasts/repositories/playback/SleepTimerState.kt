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

sealed interface SleepTimerHistory {
    val lastFinishedTime: Duration?

    data class AfterTime(
        val lastSleepAfterTime: Duration,
        override val lastFinishedTime: Duration,
    ) : SleepTimerHistory

    data class AfterChapter(
        val lastSleepAfterEndOfChapterTime: Duration,
        override val lastFinishedTime: Duration,
    ) : SleepTimerHistory

    data class AfterEpisode(
        val lastEpisodeUuidAutomaticEnded: String,
        override val lastFinishedTime: Duration,
    ) : SleepTimerHistory

    data object None : SleepTimerHistory {
        override val lastFinishedTime: Duration? = null
    }
}
