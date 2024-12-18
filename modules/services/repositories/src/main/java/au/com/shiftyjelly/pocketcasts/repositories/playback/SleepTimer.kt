package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent.PLAYER_SLEEP_TIMER_RESTARTED
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
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
        private val MIN_TIME_TO_RESTART_SLEEP_TIMER_IN_MINUTES = 1.minutes
        private const val TIME_KEY = "time"
        private const val NUMBER_OF_EPISODES_KEY = "number_of_episodes"
        private const val NUMBER_OF_CHAPTERS_KEY = "number_of_chapters"
        private const val END_OF_EPISODE_VALUE = "end_of_episode"
        private const val END_OF_CHAPTER_VALUE = "end_of_chapter"
        const val TAG: String = "SleepTimer"
    }

    private var sleepTimerHistory: SleepTimerHistory = SleepTimerHistory.None

    private val _stateFlow: MutableStateFlow<SleepTimerState> = MutableStateFlow(SleepTimerState())
    val stateFlow: StateFlow<SleepTimerState> = _stateFlow

    val state: SleepTimerState
        get() = _stateFlow.value

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

    fun sleepAfter(duration: Duration) {
        updateSleepTimerStatus(sleepTimeRunning = true, timeLeft = duration)

        sleepTimerHistory = SleepTimerHistory.AfterTime(
            lastSleepAfterTime = duration,
            lastTimeSleepTimeHasFinished = System.currentTimeMillis().milliseconds + duration,
        )
    }

    fun addExtraTime(duration: Duration) {
        val currentTimeLeft: Duration = state.timeLeft
        if (currentTimeLeft < ZERO) {
            return
        }
        val newTimeLeft = currentTimeLeft + duration

        _stateFlow.update { currentState ->
            currentState.copy(timeLeft = newTimeLeft)
        }

        LogBuffer.i(TAG, "Added extra time: $newTimeLeft")
    }

    /*
     * This restart only applies if the sleep timer is set to "sleep in x minutes".
     * Other options like "end of chapter" and "end of episode" do not apply.
     * */
    fun restartTimerForSleepAfterTime(): Duration? {
        return if (state.timeLeft != ZERO && sleepTimerHistory is SleepTimerHistory.AfterTime) {
            val sleepAfterTime = (sleepTimerHistory as SleepTimerHistory.AfterTime).lastSleepAfterTime
            sleepAfter(sleepAfterTime)
            sleepAfterTime
        } else {
            null
        }
    }

    fun restartSleepTimerIfApplies(
        currentEpisodeUuid: String,
    ) {
        if (!settings.autoSleepTimerRestart.value || sleepTimerHistory is SleepTimerHistory.None) return
        val lastTimeHasFinished = sleepTimerHistory.lastTimeSleepTimeHasFinished ?: return

        val diffTime = System.currentTimeMillis().milliseconds - lastTimeHasFinished

        when (val history = sleepTimerHistory) {
            is SleepTimerHistory.AfterChapter -> {
                if (shouldRestartSleepEndOfChapter(diffTime, state.isSleepEndOfChapterRunning)) {
                    val chapter = settings.getlastSleepEndOfChapter()
                    LogBuffer.i(TAG, "Sleep timer was restarted with end of $chapter chapter set")
                    updateSleepTimerStatus(sleepTimeRunning = true, sleepAfterChapters = chapter)
                    analyticsTracker.track(PLAYER_SLEEP_TIMER_RESTARTED, mapOf(TIME_KEY to END_OF_CHAPTER_VALUE, NUMBER_OF_CHAPTERS_KEY to settings.getlastSleepEndOfChapter()))
                }
            }

            is SleepTimerHistory.AfterEpisode -> {
                if (shouldRestartSleepEndOfEpisode(diffTime, currentEpisodeUuid, state.isSleepEndOfEpisodeRunning, history.lastEpisodeUuidAutomaticEnded)) {
                    val episodes = settings.getlastSleepEndOfEpisodes()
                    LogBuffer.i(TAG, "Sleep timer was restarted with end of $episodes episodes set")
                    updateSleepTimerStatus(sleepTimeRunning = true, sleepAfterEpisodes = episodes)
                    analyticsTracker.track(PLAYER_SLEEP_TIMER_RESTARTED, mapOf(TIME_KEY to END_OF_EPISODE_VALUE, NUMBER_OF_EPISODES_KEY to settings.getlastSleepEndOfEpisodes()))
                }
            }

            is SleepTimerHistory.AfterTime -> {
                if (shouldRestartSleepAfterTime(diffTime, state.isSleepTimerRunning)) {
                    sleepAfter(history.lastSleepAfterTime)
                    analyticsTracker.track(PLAYER_SLEEP_TIMER_RESTARTED, mapOf(TIME_KEY to history.lastSleepAfterTime.inWholeSeconds))
                    LogBuffer.i(TAG, "Was restarted with ${history.lastSleepAfterTime.inWholeMinutes} minutes set")
                }
            }

            SleepTimerHistory.None -> {}
        }
    }

    suspend fun sleepEndOfEpisode(episode: BaseEpisode, onSleepEndOfEpisode: suspend () -> Unit) {
        if (state.isSleepEndOfEpisodeRunning) {
            setEndOfEpisodeUuid(episode.uuid)
            updateSleepTimer {
                copy(numberOfEpisodesLeft = state.numberOfEpisodesLeft - 1)
            }
        }

        if (state.isSleepEndOfEpisodeRunning) return

        updateSleepTimerStatus(sleepTimeRunning = false)

        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Sleeping playback for end of episode")

        onSleepEndOfEpisode()
    }

    suspend fun sleepEndOfChapter(onSleepEndOfChapter: suspend () -> Unit) {
        if (state.isSleepEndOfChapterRunning) {
            updateSleepTimer {
                copy(numberOfChaptersLeft = state.numberOfChaptersLeft - 1)
            }
            setEndOfChapter()
        }

        if (state.isSleepEndOfChapterRunning) return

        updateSleepTimerStatus(sleepTimeRunning = false)

        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Sleeping playback for end of chapters")

        onSleepEndOfChapter()
    }

    fun cancelTimer() {
        LogBuffer.i(TAG, "Cleaning automatic sleep timer feature...")
        updateSleepTimerStatus(sleepTimeRunning = false, sleepAfterChapters = 0, sleepAfterEpisodes = 0)
        sleepTimerHistory = SleepTimerHistory.None
    }

    private fun setEndOfEpisodeUuid(uuid: String) {
        LogBuffer.i(TAG, "Episode $uuid was marked as end of episode")
        sleepTimerHistory = SleepTimerHistory.AfterEpisode(
            lastEpisodeUuidAutomaticEnded = uuid,
            lastTimeSleepTimeHasFinished = System.currentTimeMillis().milliseconds,
        )
    }

    private fun setEndOfChapter() {
        LogBuffer.i(TAG, "End of chapter was reached")
        val time = System.currentTimeMillis().milliseconds
        sleepTimerHistory = SleepTimerHistory.AfterChapter(
            lastSleepAfterEndOfChapterTime = time,
            lastTimeSleepTimeHasFinished = time,
        )
    }

    private fun shouldRestartSleepAfterTime(diffTime: Duration, isSleepTimerRunning: Boolean): Boolean = diffTime < MIN_TIME_TO_RESTART_SLEEP_TIMER_IN_MINUTES && sleepTimerHistory is SleepTimerHistory.AfterTime && !isSleepTimerRunning

    private fun shouldRestartSleepEndOfEpisode(
        diffTime: Duration,
        currentEpisodeUuid: String,
        isSleepEndOfEpisodeRunning: Boolean,
        lastEpisodeUuidAutomaticEnded: String,
    ) = diffTime < MIN_TIME_TO_RESTART_SLEEP_TIMER_IN_MINUTES && currentEpisodeUuid != lastEpisodeUuidAutomaticEnded && !isSleepEndOfEpisodeRunning

    private fun shouldRestartSleepEndOfChapter(diffTime: Duration, isSleepEndOfChapterRunning: Boolean) = diffTime < MIN_TIME_TO_RESTART_SLEEP_TIMER_IN_MINUTES && !isSleepEndOfChapterRunning

    private fun updateSleepTimer(update: SleepTimerState.() -> SleepTimerState) {
        _stateFlow.update { currentState -> currentState.update() }
    }
}
