package au.com.shiftyjelly.pocketcasts.voicecontrol.route

import au.com.shiftyjelly.pocketcasts.preferences.model.VoiceControlAudioRoutePolicy
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRule
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleGroup
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class AudioRoutePolicyRule(
    private val route: StateFlow<AudioRoute>,
    private val policy: StateFlow<VoiceControlAudioRoutePolicy>,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) : VoiceControlRule {
    override val id: String = "audio_route_policy"
    override val group = VoiceControlRuleGroup.Conflicts

    override val state: StateFlow<VoiceControlRuleState> = combine(route, policy) { route, policy ->
        evaluate(route, policy)
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = evaluate(),
    )

    fun evaluate(): VoiceControlRuleState = evaluate(route.value, policy.value)

    private fun evaluate(
        route: AudioRoute,
        policy: VoiceControlAudioRoutePolicy,
    ): VoiceControlRuleState {
        return when (policy) {
            VoiceControlAudioRoutePolicy.HeadsetOnly -> when (route) {
                AudioRoute.Headset(hasMicrophone = true),
                AudioRoute.BluetoothA2dpOnly, // SCO opened at engine start
                -> VoiceControlRuleState.Allowed

                is AudioRoute.Headset,
                AudioRoute.Speaker,
                AudioRoute.Unknown,
                -> disallowed
            }

            VoiceControlAudioRoutePolicy.SpeakerExperimental -> when (route) {
                AudioRoute.Headset(hasMicrophone = true),
                AudioRoute.BluetoothA2dpOnly, // SCO opened at engine start
                AudioRoute.Speaker,
                -> VoiceControlRuleState.Allowed

                is AudioRoute.Headset,
                AudioRoute.Unknown,
                -> disallowed
            }
        }
    }

    private companion object {
        val disallowed = VoiceControlRuleState.Blocked("audio_route_disallowed")
    }
}
