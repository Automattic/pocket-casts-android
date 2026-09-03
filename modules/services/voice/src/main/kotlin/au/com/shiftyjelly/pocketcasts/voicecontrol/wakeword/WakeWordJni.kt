package au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword

object WakeWordJni {
    init {
        System.loadLibrary("onnxruntime")
        System.loadLibrary("pocketcasts_voice_capture")
    }

    /** Initialize the wake word pipeline. Returns true on success. */
    external fun nativeInit(
        melModel: ByteArray,
        embedModel: ByteArray,
        classifierModel: ByteArray,
        threshold: Float,
    ): Boolean

    /**
     * Run detection on a complete audio segment.
     * @param samples Normalized float PCM in [-1, 1] at 16kHz.
     * @param sampleRateHz Must be 16000.
     * @param outOffset Single-element array that receives the winning window's
     *   waveform endpoint in onset-aligned samples, or -1 on error / no window.
     * @return Max classifier score across the segment, or -1 on error.
     */
    external fun nativeDetect(samples: FloatArray, sampleRateHz: Int, outOffset: FloatArray): Float

    /**
     * Debug-only: compute the log-Mel matrix for a segment.
     * @param samples Normalized float PCM in [-1, 1] at 16kHz.
     * @param sampleRateHz Must be 16000.
     * @return Flat float array of shape (time_frames * 32), or null on error.
     */
    external fun nativeGetLogMel(samples: FloatArray, sampleRateHz: Int): FloatArray?

    /** Release native resources. */
    external fun nativeRelease()
}
