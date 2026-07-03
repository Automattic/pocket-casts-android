package au.com.shiftyjelly.pocketcasts.voicecontrol.gate

import kotlinx.coroutines.flow.StateFlow

interface VoiceControlRule {
    val id: String
    val group: VoiceControlRuleGroup
    val state: StateFlow<VoiceControlRuleState>
}

sealed interface VoiceControlRuleState {
    data object Allowed : VoiceControlRuleState
    data class Blocked(val reason: String) : VoiceControlRuleState
    data class Unknown(val reason: String) : VoiceControlRuleState
}
