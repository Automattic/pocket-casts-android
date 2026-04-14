package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.widget.Toast
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Shared sleep timer management used by both [PlaybackService] and [LegacyPlaybackService].
 */
internal class SleepTimerHandler(
    private val sleepTimer: SleepTimer,
    private val playbackManager: PlaybackManager,
    private val contextProvider: () -> Context,
    private val scope: CoroutineScope,
) {
    private var sleepTimerDisposable: Disposable? = null
    private var observeJob: Job? = null

    @Volatile
    private var currentTimeLeft: Duration = ZERO

    fun observe() {
        observeJob?.cancel()
        observeJob = sleepTimer.stateFlow
            .onEach { state -> onSleepTimerStateChange(state) }
            .catch { throwable -> Timber.e(throwable, "Error observing SleepTimer state") }
            .launchIn(scope)
    }

    fun dispose() {
        observeJob?.cancel()
        observeJob = null
        cancelTimer()
    }

    private fun cancelTimer() {
        sleepTimerDisposable?.dispose()
        sleepTimerDisposable = null
        currentTimeLeft = ZERO
    }

    private fun onSleepTimerStateChange(state: SleepTimerState) {
        if (state.isSleepTimerRunning && state.timeLeft != ZERO) {
            startOrUpdateSleepTimer(state.timeLeft)
        } else {
            cancelTimer()
        }
    }

    private fun startOrUpdateSleepTimer(newTimeLeft: Duration) {
        if (newTimeLeft == ZERO || newTimeLeft.isNegative()) {
            return
        }

        if (sleepTimerDisposable == null || sleepTimerDisposable!!.isDisposed) {
            currentTimeLeft = newTimeLeft

            sleepTimerDisposable = Observable.interval(1, TimeUnit.SECONDS, Schedulers.computation())
                .takeWhile { currentTimeLeft > ZERO }
                .doOnNext {
                    currentTimeLeft = currentTimeLeft.minus(1.seconds)
                    sleepTimer.updateSleepTimerStatus(sleepTimeRunning = currentTimeLeft != ZERO, timeLeft = currentTimeLeft)

                    if (currentTimeLeft == 5.seconds) {
                        playbackManager.performVolumeFadeOut(5.0)
                    }

                    if (currentTimeLeft <= ZERO) {
                        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Paused from sleep timer.")
                        val context = contextProvider()
                        scope.launch(Dispatchers.Main) {
                            Toast.makeText(context, context.getString(R.string.player_sleep_timer_stopped_your_podcast), Toast.LENGTH_LONG).show()
                            playbackManager.restorePlayerVolume()
                        }
                        playbackManager.pause(sourceView = SourceView.AUTO_PAUSE)
                        sleepTimer.updateSleepTimerStatus(sleepTimeRunning = false)
                        cancelTimer()
                    }
                }
                .subscribe({}, { e -> Timber.e(e, "Sleep timer interval error") })
        } else {
            currentTimeLeft = newTimeLeft
        }
    }
}
