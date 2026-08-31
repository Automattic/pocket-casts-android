package au.com.shiftyjelly.pocketcasts.voicecontrol.gate.signals

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Tracks the conversation grace period that starts/resets when a command is recognized
 * or the wake word is detected (in WakeWord mode, the wake word triggers the grace period
 * so follow-up commands flow in Continuous mode).
 *
 * While the timer is active the microphone stays in Continuous mode. The grace period
 * ends when: the 30-second timer expires, the audio route changes, or the app is
 * switched away from.
 *
 * Spec: Every recognized command or wake-word detection starts/resets a 30-second
 * conversation grace period.
 */
@Singleton
class GracePeriodSignal @Inject constructor() {
    /** Internal constructor for testing with custom timeout. */
    internal constructor(timeoutMs: Long) : this() {
        this.timeoutMs = timeoutMs
    }

    private var timeoutMs: Long = 30_000L
    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    /** Called when a command is recognized — starts/resets the grace period. */
    fun onCommandRecognized() {
        startOrReset()
    }

    /** Called when the wake word is detected — starts/resets the grace period. */
    fun onWakeWordDetected() {
        startOrReset()
    }

    private fun startOrReset() {
        _isActive.value = true
        timerJob?.cancel()
        timerJob = scope.launch {
            delay(timeoutMs)
            _isActive.value = false
        }
    }

    /** Called when audio route changes — immediately ends grace period. */
    fun onAudioRouteChanged() {
        timerJob?.cancel()
        _isActive.value = false
    }

    /** Called when app goes to background — immediately ends grace period. */
    fun onAppBackgrounded() {
        timerJob?.cancel()
        _isActive.value = false
    }
}
