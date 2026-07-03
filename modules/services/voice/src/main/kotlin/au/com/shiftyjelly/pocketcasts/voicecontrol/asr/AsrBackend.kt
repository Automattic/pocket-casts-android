package au.com.shiftyjelly.pocketcasts.voicecontrol.asr

interface AsrBackend {
    suspend fun ensureReady(): Result<Unit>

    suspend fun transcribe(samples: FloatArray, sampleRateHz: Int = 16000): AsrResult

    val requiredModel: ModelSpec

    val capabilities: AsrCapabilities

    fun release()
}
