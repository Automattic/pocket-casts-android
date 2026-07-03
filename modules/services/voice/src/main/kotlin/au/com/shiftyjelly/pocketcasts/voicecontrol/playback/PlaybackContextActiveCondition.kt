package au.com.shiftyjelly.pocketcasts.voicecontrol.playback

import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRule
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleGroup
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class PlaybackContextActiveCondition(
    private val playbackContext: StateFlow<PlaybackContext>,
    scope: kotlinx.coroutines.CoroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
) : VoiceControlRule {
    override val id = "playback_context"
    override val group = VoiceControlRuleGroup.Context
    override val state: StateFlow<VoiceControlRuleState> = playbackContext
        .map { context -> evaluate(context) }
        .stateIn(
            scope,
            kotlinx.coroutines.flow.SharingStarted.Eagerly,
            evaluate(playbackContext.value),
        )

    fun evaluate(): VoiceControlRuleState = evaluate(playbackContext.value)

    internal fun evaluate(context: PlaybackContext): VoiceControlRuleState {
        return when (context) {
            is PlaybackContext.Active -> VoiceControlRuleState.Allowed
            PlaybackContext.Inactive -> VoiceControlRuleState.Blocked("playback_context_inactive")
        }
    }
}
