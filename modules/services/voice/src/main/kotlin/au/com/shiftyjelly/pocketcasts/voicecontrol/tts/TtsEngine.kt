package au.com.shiftyjelly.pocketcasts.voicecontrol.tts

interface TtsEngine {
    suspend fun warmUp(language: String)
    suspend fun speak(text: String, language: String)
    fun release()
}

class FakeTtsEngine : TtsEngine {
    var isWarm = false
        private set
    var lastSpokenText: String? = null
        private set
    var lastSpokenLanguage: String? = null
        private set
    private var released = false

    override suspend fun warmUp(language: String) {
        isWarm = true
    }

    override suspend fun speak(text: String, language: String) {
        check(!released) { "Engine is released" }
        lastSpokenText = text
        lastSpokenLanguage = language
    }

    override fun release() {
        released = true
    }
}
