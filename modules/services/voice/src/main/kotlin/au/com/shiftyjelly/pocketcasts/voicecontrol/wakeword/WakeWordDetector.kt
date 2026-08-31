package au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword

interface WakeWordDetector {
    /** Process a VAD segment and return true if wake word was detected. */
    suspend fun detect(
        segment: FloatArray,
        sampleRateHz: Int = 16000,
        speechOnsetSample: Int = 0,
    ): WakeWordResult

    /** Whether this detector is ready (model loaded, template enrolled, etc.). */
    val isReady: Boolean

    /** Release resources. */
    fun release()
}

data class WakeWordResult(
    val detected: Boolean,
    val confidence: Float = 0f,
    /** If wake word was at start of segment, remaining audio after the keyword. */
    val remainderSamples: FloatArray? = null,
    /** True when scoring failed (native error, unready detector, unsupported rate). */
    val error: Boolean = false,
)
