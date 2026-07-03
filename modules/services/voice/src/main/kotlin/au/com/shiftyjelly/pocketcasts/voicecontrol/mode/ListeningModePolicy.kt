package au.com.shiftyjelly.pocketcasts.voicecontrol.mode

import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.voicecontrol.foreground.ForegroundStateMonitor
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlGate
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlGateState
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.signals.AttendedSignal
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.signals.GracePeriodSignal
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.signals.PlaybackRecencySignal
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.PlaybackContext
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.PlaybackContextMonitor
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.AudioRoute
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.AudioRouteMonitor
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.MicExposure
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.toMicExposure
import au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword.WakeWordDetector
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

/** Bundles the first four signals so they fit in one leg of a nested [combine]. */
private data class PolicyInputs(
    val gateState: VoiceControlGateState,
    val route: AudioRoute,
    val isForeground: Boolean,
    val contextState: PlaybackContext,
)

@Singleton
class ListeningModePolicy @Inject constructor(
    private val gate: VoiceControlGate,
    private val audioRouteMonitor: AudioRouteMonitor,
    private val foregroundState: ForegroundStateMonitor,
    private val playbackContextMonitor: PlaybackContextMonitor,
    private val wakeWordDetector: WakeWordDetector,
    private val gracePeriodSignal: GracePeriodSignal,
    private val attendedSignal: AttendedSignal,
    private val playbackRecencySignal: PlaybackRecencySignal,
    @ApplicationScope private val scope: CoroutineScope,
) {
    val mode: StateFlow<ListeningMode> = combine(
        combine(
            gate.state,
            audioRouteMonitor.route,
            foregroundState.isInForeground,
            playbackContextMonitor.context,
        ) { gateState, route, isForeground, contextState ->
            PolicyInputs(gateState, route, isForeground, contextState)
        },
        gracePeriodSignal.isActive,
        attendedSignal.isAttended,
        playbackRecencySignal.isRecent,
    ) { inputs, isGracePeriodActive, isAttended, isPlaybackRecent ->
        val isPlaybackActive = inputs.contextState is PlaybackContext.Active
        resolve(
            gateState = inputs.gateState,
            micExposure = inputs.route.toMicExposure(),
            isForeground = inputs.isForeground,
            isPlaybackActive = isPlaybackActive,
            isAttended = isAttended,
            isGracePeriodActive = isGracePeriodActive,
            isPlaybackRecent = isPlaybackRecent,
            wakeWordReady = wakeWordDetector.isReady,
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = resolve(
            gateState = gate.state.value,
            micExposure = audioRouteMonitor.route.value.toMicExposure(),
            isForeground = foregroundState.isInForeground.value,
            isPlaybackActive = playbackContextMonitor.context.value is PlaybackContext.Active,
            isAttended = attendedSignal.isAttended.value,
            isGracePeriodActive = gracePeriodSignal.isActive.value,
            isPlaybackRecent = playbackRecencySignal.isRecent.value,
            wakeWordReady = wakeWordDetector.isReady,
        ),
    )
}

internal fun resolve(
    gateState: VoiceControlGateState,
    micExposure: MicExposure,
    isForeground: Boolean,
    isPlaybackActive: Boolean,
    isAttended: Boolean,
    isGracePeriodActive: Boolean,
    isPlaybackRecent: Boolean,
    wakeWordReady: Boolean,
): ListeningMode {
    if (!gateState.allowed) {
        Timber.d("Mode: Off (gate blocked)")
        return ListeningMode.Off
    }
    if (micExposure == MicExposure.NoMic) {
        Timber.d("Mode: Off (no mic)")
        return ListeningMode.Off
    }

    // Priority 1: Conversation grace period overrides everything
    if (isGracePeriodActive) {
        Timber.d("Mode: Continuous (grace period)")
        return ListeningMode.Continuous
    }

    // Priority 2: Foreground + attended -> Continuous
    if (isForeground && isAttended) {
        Timber.d("Mode: Continuous (foreground + attended)")
        return ListeningMode.Continuous
    }

    // Priority 3: Foreground + unattended -> WakeWord
    if (isForeground && !isAttended) {
        Timber.d("Mode: WakeWord (foreground + unattended)")
        return ListeningMode.WakeWord
    }

    // Priority 4: Background + Isolated + playback active/recent -> Continuous
    if (!isForeground && isPlaybackActive && micExposure == MicExposure.Isolated && isPlaybackRecent) {
        Timber.d("Mode: Continuous (bg + Isolated + playback recent)")
        return ListeningMode.Continuous
    }

    // Priority 5: Background + Isolated + playback inactive >30s -> WakeWord
    if (!isForeground && isPlaybackActive && micExposure == MicExposure.Isolated && !isPlaybackRecent) {
        Timber.d("Mode: WakeWord (bg + Isolated + playback not recent)")
        return ListeningMode.WakeWord
    }

    // Priority 6: Background + Exposed -> WakeWord
    if (!isForeground && isPlaybackActive && micExposure == MicExposure.Exposed) {
        if (!wakeWordReady) {
            Timber.d("Mode: Off (bg + Exposed, wake word not ready)")
            return ListeningMode.Off
        }
        Timber.d("Mode: WakeWord (bg + Exposed)")
        return ListeningMode.WakeWord
    }

    Timber.d(
        "Mode: Off (no match) fg=%b attended=%b playback=%b exposure=%s recent=%b grace=%b",
        isForeground,
        isAttended,
        isPlaybackActive,
        micExposure,
        isPlaybackRecent,
        isGracePeriodActive,
    )
    return ListeningMode.Off
}
