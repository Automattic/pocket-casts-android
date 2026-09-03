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
        // Without token timestamps we cannot strip by time band; treat as wake-only
        // so the wake phrase never reaches the classifier.
        val tokens = result.tokens ?: return ""
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
