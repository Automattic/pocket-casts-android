package au.com.shiftyjelly.pocketcasts.voicecontrol.gate.conditions

import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRule
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleGroup
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DeviceSupportedCondition(
    private val apiLevel: Int = android.os.Build.VERSION.SDK_INT,
    private val sufficientRam: Boolean = true,
) : VoiceControlRule {

    override val id = "device_supported"
    override val group = VoiceControlRuleGroup.Setup

    private val mutableState = MutableStateFlow(evaluate())
    override val state: StateFlow<VoiceControlRuleState> = mutableState

    fun evaluate(): VoiceControlRuleState {
        return if (apiLevel >= 26 && sufficientRam) {
            VoiceControlRuleState.Allowed
        } else {
            VoiceControlRuleState.Blocked("device_unsupported")
        }
    }
}
