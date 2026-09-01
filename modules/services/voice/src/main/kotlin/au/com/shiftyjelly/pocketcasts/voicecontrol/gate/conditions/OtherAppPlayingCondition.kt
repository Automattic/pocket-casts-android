package au.com.shiftyjelly.pocketcasts.voicecontrol.gate.conditions

import android.media.AudioManager
import android.os.SystemClock
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRule
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleGroup
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OtherAppPlayingCondition(
    private val audioManager: AudioManager? = null,
    private val hostIsPlaying: StateFlow<Boolean> = MutableStateFlow(false),
    private val nowMs: () -> Long = { SystemClock.elapsedRealtime() },
    private val transitionWindowMs: Long = 5_000,
    private val debounceMs: Long = 500,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : VoiceControlRule {

    override val id = "other_app_playing"
    override val group = VoiceControlRuleGroup.Conflicts

    private val mutableState = MutableStateFlow<VoiceControlRuleState>(VoiceControlRuleState.Unknown("initializing"))
    override val state: StateFlow<VoiceControlRuleState> = mutableState

    private var debounceJob: Job? = null

    // Last time (monotonic ms) the host was observed actively playing, used for a
    // bounded attribution window so a play/pause/route transition (where isMusicActive
    // is true but isPlaying has not yet flipped) is still attributed to the host without
    // permanently treating a long-paused loaded episode as host-owned.
    private var lastHostPlayingMs: Long = Long.MIN_VALUE

    init {
        if (audioManager != null) {
            scope.launch { pollLoop() }
        }
    }

    private suspend fun pollLoop() {
        while (true) {
            val hasOtherApp = withContext(Dispatchers.IO) {
                val am = audioManager ?: return@withContext false
                otherAppPlaying(am.isMusicActive, hostIsPlaying.value, nowMs() - lastHostPlayingMs, transitionWindowMs)
            }
            if (hostIsPlaying.value) {
                lastHostPlayingMs = nowMs()
            }
            handleStateChange(hasOtherApp)
            delay(1000)
        }
    }

    /**
     * Directly updates the condition state, bypassing the AudioManager.
     * Used for testing and by external state monitors.
     */
    fun update(otherAppPlaying: Boolean) {
        handleStateChange(otherAppPlaying)
    }

    private fun handleStateChange(hasOtherApp: Boolean) {
        debounceJob?.cancel()
        if (hasOtherApp) {
            debounceJob = scope.launch {
                delay(debounceMs)
                mutableState.value = VoiceControlRuleState.Blocked("other_app_playing")
            }
        } else {
            mutableState.value = VoiceControlRuleState.Allowed
        }
    }

    fun evaluate(): VoiceControlRuleState {
        val hasOtherApp = audioManager?.let { am ->
            otherAppPlaying(am.isMusicActive, hostIsPlaying.value, nowMs() - lastHostPlayingMs, transitionWindowMs)
        } ?: false
        return evaluate(hasOtherApp)
    }

    internal fun evaluate(otherAppPlaying: Boolean): VoiceControlRuleState {
        return if (otherAppPlaying) {
            VoiceControlRuleState.Blocked("other_app_playing")
        } else {
            VoiceControlRuleState.Allowed
        }
    }
}

/**
 * Decides whether "another app is playing": audio is active ([isMusicActive]) and the
 * host does not own it. The host owns audio when it is currently playing
 * ([hostCurrentlyPlaying]) or was playing within [transitionWindowMs] (i.e.
 * [msSinceHostPlaying] < window). The window covers the play/pause/route transition
 * where `AudioManager.isMusicActive()` is true while the host's `isPlaying` has not yet
 * flipped — previously that misattributed the host's own audio to "another app" and
 * blocked the mic. Bounding the window (rather than treating any loaded episode as
 * host-owned) keeps the gate blocking when a genuinely different app plays while the
 * host is long-paused.
 */
internal fun otherAppPlaying(
    isMusicActive: Boolean,
    hostCurrentlyPlaying: Boolean,
    msSinceHostPlaying: Long,
    transitionWindowMs: Long,
): Boolean {
    if (!isMusicActive) return false
    val hostOwnsAudio = hostCurrentlyPlaying || (msSinceHostPlaying >= 0 && msSinceHostPlaying < transitionWindowMs)
    return !hostOwnsAudio
}
