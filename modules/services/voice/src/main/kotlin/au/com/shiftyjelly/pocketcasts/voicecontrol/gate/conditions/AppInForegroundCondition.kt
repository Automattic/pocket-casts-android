package au.com.shiftyjelly.pocketcasts.voicecontrol.gate.conditions

import au.com.shiftyjelly.pocketcasts.voicecontrol.foreground.ForegroundStateMonitor
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRule
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleGroup
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AppInForegroundCondition(
    private val foregroundState: ForegroundStateMonitor,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) : VoiceControlRule {

    override val id = "app_in_foreground"
    override val group = VoiceControlRuleGroup.Context

    override val state: StateFlow<VoiceControlRuleState> = foregroundState.isInForeground
        .map { foreground -> evaluate(foreground) }
        .stateIn(scope, SharingStarted.Eagerly, evaluate(foregroundState.isInForeground.value))

    fun evaluate(): VoiceControlRuleState = evaluate(foregroundState.isInForeground.value)

    internal fun evaluate(isForeground: Boolean): VoiceControlRuleState {
        return if (isForeground) {
            VoiceControlRuleState.Allowed
        } else {
            VoiceControlRuleState.Blocked("not_foreground")
        }
    }
}
