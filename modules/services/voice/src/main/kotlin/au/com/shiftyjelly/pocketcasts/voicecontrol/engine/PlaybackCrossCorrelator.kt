package au.com.shiftyjelly.pocketcasts.voicecontrol.engine

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Cross-correlates mic audio against the playback buffer to detect
 * podcast bleed-through. When the mic signal strongly correlates with
 * what the device is playing, the utterance is classified as bleed.
 */
@Singleton
class PlaybackCrossCorrelator @Inject constructor() {

    fun isPlaybackBleed(
        micAudio: FloatArray,
        playbackBuffer: FloatArray,
    ): Boolean {
        if (playbackBuffer.size < micAudio.size) return false

        var maxCorrelation = 0.0
        val minDelay = (0.050 * 16000).toInt()
        val maxDelay = (0.500 * 16000).toInt()

        for (offset in minDelay..maxDelay.coerceAtMost(playbackBuffer.size - micAudio.size)) {
            val correlation = normalizedCrossCorrelation(micAudio, playbackBuffer, offset)
            if (correlation > maxCorrelation) maxCorrelation = correlation
        }

        return maxCorrelation > BLEED_THRESHOLD
    }

    private fun normalizedCrossCorrelation(
        signal: FloatArray,
        reference: FloatArray,
        offset: Int,
    ): Double {
        var dot = 0.0
        var normSignal = 0.0
        var normRef = 0.0
        for (i in signal.indices) {
            val s = signal[i].toDouble()
            val r = reference[offset + i].toDouble()
            dot += s * r
            normSignal += s * s
            normRef += r * r
        }
        val denom = sqrt(normSignal) * sqrt(normRef)
        return if (denom == 0.0) 0.0 else dot / denom
    }

    companion object {
        private const val BLEED_THRESHOLD = 0.3
    }
}
