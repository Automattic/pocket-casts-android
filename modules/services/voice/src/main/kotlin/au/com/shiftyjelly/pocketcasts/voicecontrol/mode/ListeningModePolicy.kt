package au.com.shiftyjelly.pocketcasts.voicecontrol.mode

import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlGate
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlGateState
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.signals.GracePeriodSignal
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.AudioRouteMonitor
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.MicExposure
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.toMicExposure
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

@Singleton
class ListeningModePolicy @Inject constructor(
    private val gate: VoiceControlGate,
    private val audioRouteMonitor: AudioRouteMonitor,
    private val gracePeriodSignal: GracePeriodSignal,
    @ApplicationScope private val scope: CoroutineScope,
) {
    val mode: StateFlow<ListeningMode> = combine(
        gate.state,
        audioRouteMonitor.route,
        gracePeriodSignal.isActive,
    ) { gateState, route, isGracePeriodActive ->
        resolve(
            gateState = gateState,
            micExposure = route.toMicExposure(),
            isGracePeriodActive = isGracePeriodActive,
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = resolve(
            gateState = gate.state.value,
            micExposure = audioRouteMonitor.route.value.toMicExposure(),
            isGracePeriodActive = gracePeriodSignal.isActive.value,
        ),
    )
}

internal fun resolve(
    gateState: VoiceControlGateState,
    micExposure: MicExposure,
    isGracePeriodActive: Boolean,
): ListeningMode {
    if (!gateState.allowed) {
        Timber.d("[VoicePipeline] mode Off (gate blocked)")
        return ListeningMode.Off
    }
    if (micExposure == MicExposure.NoMic) {
        Timber.d("[VoicePipeline] mode Off (no mic)")
        return ListeningMode.Off
    }

    // Grace period is the ONLY wake-word waiver
    if (isGracePeriodActive) {
        Timber.d("[VoicePipeline] mode Continuous (grace period)")
        return ListeningMode.Continuous
    }

    Timber.d("[VoicePipeline] mode WakeWord")
    return ListeningMode.WakeWord
}
