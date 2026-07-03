package au.com.shiftyjelly.pocketcasts.voicecontrol.engine

import au.com.shiftyjelly.pocketcasts.voicecontrol.route.AudioRoute
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.AudioRouteMonitor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Filters utterances before they reach the intent parser.
 *
 * Two checks:
 * 1. Speaker consistency — Moonshine diarization via [identify_speakers].
 *    First accepted command in a listening session establishes the target speaker;
 *    subsequent utterances from other speaker indices are dropped.
 * 2. Playback bleed rejection — cross-correlation of mic audio against the
 *    playback buffer. Disabled when using a headset (no acoustic path from
 *    speaker to mic).
 */
@Singleton
class UtteranceFilter @Inject constructor(
    private val playbackCorrelator: PlaybackCrossCorrelator,
    private val audioRouteMonitor: AudioRouteMonitor,
) {
    private var sessionTargetSpeaker: Int? = null

    fun reset() {
        sessionTargetSpeaker = null
    }

    fun shouldProcess(
        audio: FloatArray,
        hasSpeakerId: Boolean,
        speakerIndex: Int,
        playbackBuffer: FloatArray,
    ): Boolean {
        if (hasSpeakerId) {
            if (sessionTargetSpeaker == null) {
                sessionTargetSpeaker = speakerIndex
            } else if (speakerIndex != sessionTargetSpeaker) {
                return false
            }
        }

        if (audioRouteMonitor.route.value !is AudioRoute.Headset) {
            if (playbackCorrelator.isPlaybackBleed(audio, playbackBuffer)) {
                return false
            }
        }

        return true
    }
}
