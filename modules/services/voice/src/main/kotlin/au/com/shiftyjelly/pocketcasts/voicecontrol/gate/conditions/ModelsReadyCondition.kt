package au.com.shiftyjelly.pocketcasts.voicecontrol.gate.conditions

import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRule
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleGroup
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ModelsReadyCondition(
    private val isReady: Boolean = true,
    private val failed: Boolean = false,
) : VoiceControlRule {

    override val id = "models_ready"
    override val group = VoiceControlRuleGroup.Setup

    private val mutableState = MutableStateFlow(evaluate())
    override val state: StateFlow<VoiceControlRuleState> = mutableState

    fun update(isReady: Boolean, failed: Boolean = false) {
        mutableState.value = evaluate(isReady, failed)
    }

    fun evaluate(): VoiceControlRuleState = evaluate(isReady, failed)

    internal fun evaluate(isReady: Boolean, failed: Boolean): VoiceControlRuleState {
        return when {
            isReady -> VoiceControlRuleState.Allowed
            failed -> VoiceControlRuleState.Blocked("model_download_failed")
            else -> VoiceControlRuleState.Unknown("models_loading")
        }
    }
}
