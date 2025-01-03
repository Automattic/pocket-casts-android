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

sealed class SleepTimerHistory(
    open val lastFinishedTime: Duration?,
) {
    data class AfterTime(
        val lastSleepAfterTime: Duration,
        override val lastFinishedTime: Duration,
    ) : SleepTimerHistory(lastFinishedTime = lastFinishedTime)

    data class AfterChapter(
        val lastSleepAfterEndOfChapterTime: Duration,
        override val lastFinishedTime: Duration,
    ) : SleepTimerHistory(lastFinishedTime = lastFinishedTime)

    data class AfterEpisode(
        val lastEpisodeUuidAutomaticEnded: String,
        override val lastFinishedTime: Duration,
    ) : SleepTimerHistory(lastFinishedTime = lastFinishedTime)

    data object None : SleepTimerHistory(lastFinishedTime = null)
}

data class LastListenedState(
    val chapterUuid: String? = null,
    val episodeUuid: String? = null,
)
