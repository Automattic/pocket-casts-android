package au.com.shiftyjelly.pocketcasts.voicecontrol.gate.conditions

import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRule
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleGroup
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BatteryOkCondition(
    private val isPowerSaveMode: Boolean = false,
) : VoiceControlRule {

    override val id = "battery_ok"
    override val group = VoiceControlRuleGroup.Conflicts

    private val mutableState = MutableStateFlow(evaluate())
    override val state: StateFlow<VoiceControlRuleState> = mutableState

    fun updatePowerSaveMode(powerSaveMode: Boolean) {
        mutableState.value = evaluate(powerSaveMode)
    }

    fun evaluate(): VoiceControlRuleState = evaluate(isPowerSaveMode)

    internal fun evaluate(isPowerSaveMode: Boolean): VoiceControlRuleState {
        return if (isPowerSaveMode) {
            VoiceControlRuleState.Blocked("battery_saver")
        } else {
            VoiceControlRuleState.Allowed
        }
    }
}
