package au.com.shiftyjelly.pocketcasts.voicecontrol.asr

object WhisperNative {
    init {
        System.loadLibrary("pocketcasts_voice_capture")
    }

    /**
     * Pre-loads the whisper model into memory. Returns true if the model was loaded
     * successfully. Subsequent [transcribe] calls reuse the cached context.
     */
    external fun init(modelPath: String): Boolean

    external fun transcribe(
        modelPath: String,
        pcmData: ShortArray,
        sampleRate: Int,
    ): String

    external fun setPipelineCachePath(cachePath: String)

    external fun freeModel()
}
