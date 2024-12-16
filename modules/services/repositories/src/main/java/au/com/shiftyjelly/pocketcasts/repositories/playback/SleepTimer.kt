package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent.PLAYER_SLEEP_TIMER_RESTARTED
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@Singleton
class SleepTimer @Inject constructor(
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTracker,
) {
    companion object {
        private val MIN_TIME_TO_RESTART_SLEEP_TIMER_IN_MINUTES = 5.minutes
        private const val TIME_KEY = "time"
        private const val NUMBER_OF_EPISODES_KEY = "number_of_episodes"
        private const val NUMBER_OF_CHAPTERS_KEY = "number_of_chapters"
        private const val END_OF_EPISODE_VALUE = "end_of_episode"
        private const val END_OF_CHAPTER_VALUE = "end_of_chapter"
        const val TAG: String = "SleepTimer"
    }

    private var lastSleepAfterTime: Duration? = null
    private var lastSleepAfterEndOfChapterTime: Duration? = null
    private var lastTimeSleepTimeHasFinished: Duration? = null
    private var lastEpisodeUuidAutomaticEnded: String? = null

    private val _stateFlow: MutableStateFlow<SleepTimerState> = MutableStateFlow(SleepTimerState())
    val stateFlow: StateFlow<SleepTimerState> = _stateFlow

    fun getState(): SleepTimerState = _stateFlow.value

    fun updateSleepTimerStatus(
        sleepTimeRunning: Boolean,
        sleepAfterEpisodes: Int = 0,
        sleepAfterChapters: Int = 0,
        timeLeft: Duration? = null,
    ) {
        _stateFlow.update { currentState ->
            currentState.copy(
                isSleepTimerRunning = sleepTimeRunning,
                numberOfEpisodesLeft = sleepAfterEpisodes,
                numberOfChaptersLeft = sleepAfterChapters,
                timeLeft = if (!sleepTimeRunning || sleepAfterEpisodes != 0 || sleepAfterChapters != 0) {
                    ZERO
                } else {
                    timeLeft ?: currentState.timeLeft
                },
            )
        }
    }

    fun updateSleepTimerEndOfEpisodes(sleepAfterEpisodes: Int) {
        updateSleepTimer {
            copy(numberOfEpisodesLeft = sleepAfterEpisodes)
        }
    }

    fun updateSleepTimerEndOfChapters(sleepAfterChapters: Int) {
        updateSleepTimer {
            copy(numberOfChaptersLeft = sleepAfterChapters)
        }
    }

    fun sleepAfter(duration: Duration, onSuccess: () -> Unit) {
        updateSleepTimerStatus(sleepTimeRunning = true, timeLeft = duration)

        lastSleepAfterTime = duration
        cancelAutomaticSleepOnEpisodeEndRestart()
        cancelAutomaticSleepOnChapterEndRestart()

        lastTimeSleepTimeHasFinished = System.currentTimeMillis().milliseconds + duration

        onSuccess()
    }

    fun addExtraTime(duration: Duration) {
        val currentTimeLeft: Duration = getState().timeLeft
        if (currentTimeLeft < ZERO) {
            return
        }
        val newTimeLeft = currentTimeLeft + duration

        _stateFlow.update { currentState ->
            currentState.copy(timeLeft = newTimeLeft)
        }

        LogBuffer.i(TAG, "Added extra time: $newTimeLeft")
    }

    fun restartTimerIfIsRunning(onSuccess: () -> Unit): Duration? {
        return if (getState().timeLeft != ZERO) {
            lastSleepAfterTime?.let { sleepAfter(it, onSuccess) }
            lastSleepAfterTime
        } else {
            null
        }
    }

    fun restartSleepTimerIfApplies(
        autoSleepTimerEnabled: Boolean,
        currentEpisodeUuid: String,
        timerState: SleepTimerState,
        onRestartSleepAfterTime: () -> Unit,
        onRestartSleepOnEpisodeEnd: () -> Unit,
        onRestartSleepOnChapterEnd: () -> Unit,
    ) {
        if (!autoSleepTimerEnabled) return

        lastTimeSleepTimeHasFinished?.let { lastTimeHasFinished ->
            val diffTime = System.currentTimeMillis().milliseconds - lastTimeHasFinished

            if (shouldRestartSleepEndOfChapter(diffTime, timerState.isSleepEndOfChapterRunning)) {
                onRestartSleepOnChapterEnd()
                analyticsTracker.track(PLAYER_SLEEP_TIMER_RESTARTED, mapOf(TIME_KEY to END_OF_CHAPTER_VALUE, NUMBER_OF_CHAPTERS_KEY to settings.getlastSleepEndOfChapter()))
            } else if (shouldRestartSleepEndOfEpisode(diffTime, currentEpisodeUuid, timerState.isSleepEndOfEpisodeRunning)) {
                onRestartSleepOnEpisodeEnd()
                analyticsTracker.track(PLAYER_SLEEP_TIMER_RESTARTED, mapOf(TIME_KEY to END_OF_EPISODE_VALUE, NUMBER_OF_EPISODES_KEY to settings.getlastSleepEndOfEpisodes()))
            } else if (shouldRestartSleepAfterTime(diffTime, timerState.isSleepTimerRunning)) {
                lastSleepAfterTime?.let {
                    analyticsTracker.track(PLAYER_SLEEP_TIMER_RESTARTED, mapOf(TIME_KEY to it.inWholeSeconds))
                    LogBuffer.i(TAG, "Was restarted with ${it.inWholeMinutes} minutes set")
                    sleepAfter(it, onRestartSleepAfterTime)
                }
            }
        }
    }

    fun setEndOfEpisodeUuid(uuid: String) {
        LogBuffer.i(TAG, "Episode $uuid was marked as end of episode")
        lastEpisodeUuidAutomaticEnded = uuid
        lastTimeSleepTimeHasFinished = System.currentTimeMillis().milliseconds
        cancelAutomaticSleepAfterTimeRestart()
        cancelAutomaticSleepOnChapterEndRestart()
    }

    fun setEndOfChapter() {
        LogBuffer.i(TAG, "End of chapter was reached")
        val time = System.currentTimeMillis().milliseconds
        lastSleepAfterEndOfChapterTime = time
        lastTimeSleepTimeHasFinished = time
        cancelAutomaticSleepAfterTimeRestart()
        cancelAutomaticSleepOnEpisodeEndRestart()
    }

    private fun shouldRestartSleepAfterTime(diffTime: Duration, isSleepTimerRunning: Boolean) = diffTime < MIN_TIME_TO_RESTART_SLEEP_TIMER_IN_MINUTES && lastSleepAfterTime != null && !isSleepTimerRunning

    private fun shouldRestartSleepEndOfEpisode(
        diffTime: Duration,
        currentEpisodeUuid: String,
        isSleepEndOfEpisodeRunning: Boolean,
    ) = diffTime < MIN_TIME_TO_RESTART_SLEEP_TIMER_IN_MINUTES && !lastEpisodeUuidAutomaticEnded.isNullOrEmpty() && currentEpisodeUuid != lastEpisodeUuidAutomaticEnded && !isSleepEndOfEpisodeRunning

    private fun shouldRestartSleepEndOfChapter(diffTime: Duration, isSleepEndOfChapterRunning: Boolean) = diffTime < MIN_TIME_TO_RESTART_SLEEP_TIMER_IN_MINUTES && !isSleepEndOfChapterRunning && lastSleepAfterEndOfChapterTime != null

    fun cancelTimer() {
        LogBuffer.i(TAG, "Cleaning automatic sleep timer feature...")
        updateSleepTimerStatus(sleepTimeRunning = false, sleepAfterChapters = 0, sleepAfterEpisodes = 0)
        cancelAutomaticSleepAfterTimeRestart()
        cancelAutomaticSleepOnEpisodeEndRestart()
        cancelAutomaticSleepOnChapterEndRestart()
    }

    private fun updateSleepTimer(update: SleepTimerState.() -> SleepTimerState) {
        _stateFlow.update { currentState -> currentState.update() }
    }

    private fun cancelAutomaticSleepAfterTimeRestart() {
        lastSleepAfterTime = null
    }

    private fun cancelAutomaticSleepOnEpisodeEndRestart() {
        lastEpisodeUuidAutomaticEnded = null
    }

    private fun cancelAutomaticSleepOnChapterEndRestart() {
        lastSleepAfterEndOfChapterTime = null
    }
}
