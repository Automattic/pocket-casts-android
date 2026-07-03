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
    private val hostAudioPlaying: StateFlow<Boolean> = MutableStateFlow(false),
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
                // Android's isMusicActive() returns true for any audio, including the host app.
                // Filter out the host app's own playback by checking hostAudioPlaying state.
                val isMusicActive = am.isMusicActive
                isMusicActive && !hostAudioPlaying.value
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
            isMusicActive && !hostAudioPlaying.value
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
