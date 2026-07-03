package au.com.shiftyjelly.pocketcasts.voicecontrol.gate.conditions

import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRule
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleGroup
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NotCastingCondition(
    private val isCasting: Boolean = false,
) : VoiceControlRule {

    override val id = "not_casting"
    override val group = VoiceControlRuleGroup.Conflicts

    private val mutableState = MutableStateFlow(evaluate())
    override val state: StateFlow<VoiceControlRuleState> = mutableState

    fun updateCasting(casting: Boolean) {
        mutableState.value = evaluate(casting)
    }

    fun evaluate(): VoiceControlRuleState = evaluate(isCasting)

    internal fun evaluate(isCasting: Boolean): VoiceControlRuleState {
        return if (isCasting) {
            VoiceControlRuleState.Blocked("casting")
        } else {
            VoiceControlRuleState.Allowed
        }
    }
}
