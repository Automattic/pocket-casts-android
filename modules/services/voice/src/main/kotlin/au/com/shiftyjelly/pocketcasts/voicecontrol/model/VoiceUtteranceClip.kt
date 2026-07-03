package au.com.shiftyjelly.pocketcasts.voicecontrol.model

import au.com.shiftyjelly.pocketcasts.voicecontrol.audio.PcmAudioFrame

data class VoiceUtteranceClip(
    val frames: List<PcmAudioFrame>,
    val durationMs: Long,
    val confidenceScore: Float,
    val startTimeMs: Long,
    val endTimeMs: Long,
) {
    val sampleRateHz: Int
        get() = frames.firstOrNull()?.sampleRateHz ?: 16_000

    companion object {
        fun fromFrames(
            frames: List<PcmAudioFrame>,
            confidenceScore: Float = 1.0f,
            startTimeMs: Long = System.currentTimeMillis(),
        ): VoiceUtteranceClip {
            val durationMs = calculateDurationMs(frames)
            val endTimeMs = startTimeMs + durationMs
            return VoiceUtteranceClip(
                frames = frames,
                durationMs = durationMs,
                confidenceScore = confidenceScore,
                startTimeMs = startTimeMs,
                endTimeMs = endTimeMs,
            )
        }

        private fun calculateDurationMs(frames: List<PcmAudioFrame>): Long {
            if (frames.isEmpty()) return 0L
            val samplesPerFrame = frames.first().samples.size
            val sampleRateHz = frames.first().sampleRateHz
            val totalSamples = frames.size * samplesPerFrame
            return (totalSamples * 1000L) / sampleRateHz
        }
    }
}
