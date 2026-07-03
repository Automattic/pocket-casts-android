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

@Singleton
class AttendedSignal @Inject constructor(
    private val timeoutMs: Long,
) {
    private val _isAttended = MutableStateFlow(false)
    val isAttended: StateFlow<Boolean> = _isAttended

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    /** Call from the Activity-dispatch touch-event path. */
    fun onUserInteraction() {
        _isAttended.value = true
        timerJob?.cancel()
        timerJob = scope.launch {
            delay(timeoutMs)
            _isAttended.value = false
        }
    }
}
