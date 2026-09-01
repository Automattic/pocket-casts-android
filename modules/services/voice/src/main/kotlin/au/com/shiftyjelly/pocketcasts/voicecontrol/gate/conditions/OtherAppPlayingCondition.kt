package au.com.shiftyjelly.pocketcasts.voicecontrol.gate.conditions

import android.media.AudioManager
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
    private val hostHasActiveContext: StateFlow<Boolean> = MutableStateFlow(false),
    private val debounceMs: Long = 500,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : VoiceControlRule {

    override val id = "other_app_playing"
    override val group = VoiceControlRuleGroup.Conflicts

    private val mutableState = MutableStateFlow<VoiceControlRuleState>(VoiceControlRuleState.Unknown("initializing"))
    override val state: StateFlow<VoiceControlRuleState> = mutableState

    private var debounceJob: Job? = null

    init {
        if (audioManager != null) {
            scope.launch { pollLoop() }
        }
    }

    private suspend fun pollLoop() {
        while (true) {
            val hasOtherAppPlaying = withContext(Dispatchers.IO) {
                val am = audioManager ?: return@withContext false
                // Per spec: NoOtherAppPlaying must allow when Auris owns the audio output.
                // Android's isMusicActive() returns true for any audio, including the host app,
                // so we must exclude the host. We attribute an active audio session to the host
                // whenever it holds a playback context (playing OR paused) — not only when it is
                // actively playing. Using isPlaying alone is brittle: during a play/pause/route
                // transition isMusicActive can be true while isPlaying has not yet flipped, which
                // would misattribute the host's own audio to "another app" and block the mic.
                val isMusicActive = am.isMusicActive
                otherAppPlaying(isMusicActive, hostHasActiveContext.value)
            }
            handleStateChange(hasOtherAppPlaying)
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
            val isMusicActive = am.isMusicActive
            otherAppPlaying(isMusicActive, hostHasActiveContext.value)
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
 * host does not hold a playback context ([hostHasActiveContext]). When the host holds a
 * context (playing or paused) the active audio session is attributed to the host, not to
 * another app, so the mic is not blocked over the host's own audio. This is the fix for
 * the false positive where `AudioManager.isMusicActive()` is true (the host is audibly
 * playing) while the host's `isPlaying` flag is momentarily false (play/pause/route
 * transition), which previously misattributed the host's own audio to "another app".
 */
internal fun otherAppPlaying(isMusicActive: Boolean, hostHasActiveContext: Boolean): Boolean {
    return isMusicActive && !hostHasActiveContext
}
