package au.com.shiftyjelly.pocketcasts.voicecontrol.gate.conditions

import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRule
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleGroup
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NotOnCallCondition(
    private val isInCall: Boolean = false,
) : VoiceControlRule {

    override val id = "not_on_call"
    override val group = VoiceControlRuleGroup.Conflicts

    private val mutableState = MutableStateFlow(evaluate())
    override val state: StateFlow<VoiceControlRuleState> = mutableState

    fun updateInCall(inCall: Boolean) {
        mutableState.value = evaluate(inCall)
    }

    fun evaluate(): VoiceControlRuleState = evaluate(isInCall)

    internal fun evaluate(isInCall: Boolean): VoiceControlRuleState {
        return if (isInCall) {
            VoiceControlRuleState.Blocked("on_call")
        } else {
            VoiceControlRuleState.Allowed
        }
    }
}
