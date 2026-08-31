package au.com.shiftyjelly.pocketcasts.voicecontrol.gate.conditions

import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRule
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleGroup
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleState
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.signals.GracePeriodSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class GracePeriodActiveCondition(
    private val gracePeriodSignal: GracePeriodSignal,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) : VoiceControlRule {

    override val id = "grace_period_active"
    override val group = VoiceControlRuleGroup.Context

    override val state: StateFlow<VoiceControlRuleState> = gracePeriodSignal.isActive
        .map { isActive -> evaluate(isActive) }
        .stateIn(scope, SharingStarted.Eagerly, evaluate(gracePeriodSignal.isActive.value))

    fun evaluate(): VoiceControlRuleState = evaluate(gracePeriodSignal.isActive.value)

    internal fun evaluate(isActive: Boolean): VoiceControlRuleState {
        return if (isActive) {
            VoiceControlRuleState.Allowed
        } else {
            VoiceControlRuleState.Blocked("grace_inactive")
        }
    }
}
