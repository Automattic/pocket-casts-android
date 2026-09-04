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
    /** Winning classifier window endpoint in the original VAD segment (samples). */
    val completionSample: Int = 0,
    /** True when scoring failed (native error, unready detector, unsupported rate). */
    val error: Boolean = false,
    /** Detector threshold used for [detected], when known. */
    val threshold: Float = Float.NaN,
)
