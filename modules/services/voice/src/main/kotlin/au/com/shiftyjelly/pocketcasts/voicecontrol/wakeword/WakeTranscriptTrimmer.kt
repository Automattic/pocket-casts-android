package au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword

import au.com.shiftyjelly.pocketcasts.voicecontrol.asr.AsrResult

/**
 * Drops ASR tokens that overlap the detector completion band. Never strips by spelling.
 * Pad matches recognition-pipeline.md (120ms hop-jitter allowance).
 */
object WakeTranscriptTrimmer {
    const val PAD_MS = 120

    fun commandText(
        result: AsrResult,
        wakePositive: Boolean,
        completionSample: Int,
        sampleRateHz: Int,
        utteranceDurationMs: Int,
    ): String {
        if (!wakePositive) return result.text.trim()
        // Backends that never emit timestamps (e.g. SenseVoice) must keep the raw
        // transcript so wake+command still reaches the classifier. Time-band trim
        // only applies when tokens are present.
        val tokens = result.tokens ?: return result.text.trim()
        val rate = sampleRateHz.coerceAtLeast(1)
        val completionMs = ((completionSample.toLong() * 1000L) / rate).toInt()
        val bandEndMs = (completionMs + PAD_MS).let { end ->
            if (utteranceDurationMs > 0) end.coerceIn(0, utteranceDurationMs) else end.coerceAtLeast(0)
        }
        return tokens
            .filterNot { it.startMs < bandEndMs && it.endMs > 0 }
            .joinToString("") { it.text }
            .trim()
    }
}
