package au.com.shiftyjelly.pocketcasts.voicecontrol.gate

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class EnabledByUserCondition(
    settings: Settings,
    scope: kotlinx.coroutines.CoroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
) : VoiceControlRule {
    override val id = "user_not_disabled"
    override val group = VoiceControlRuleGroup.Setup

    override val state: StateFlow<VoiceControlRuleState> = settings.voiceControlUserDisabled.flow
        .map { disabled ->
            if (disabled) {
                VoiceControlRuleState.Blocked("user_disabled")
            } else {
                VoiceControlRuleState.Allowed
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, VoiceControlRuleState.Allowed)
}
