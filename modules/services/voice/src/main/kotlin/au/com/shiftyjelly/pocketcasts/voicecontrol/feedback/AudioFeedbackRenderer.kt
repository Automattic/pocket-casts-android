package au.com.shiftyjelly.pocketcasts.voicecontrol.feedback

import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.VoiceResponse
import au.com.shiftyjelly.pocketcasts.voicecontrol.tts.TtsEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AudioFeedbackRenderer(
    private val earconPlayer: EarconPlayer,
    private val ttsEngine: TtsEngine,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var currentJob: Job? = null
    private var released = false

    fun render(response: VoiceResponse, language: String = "en") {
        if (released) return
        currentJob?.cancel()
        // Launched coroutine inherits the Job cancellation: when we cancel currentJob
        // above, any child suspend calls (like TtsEngine.speak) will be cancelled.
        currentJob = scope.launch {
            when (response) {
                is VoiceResponse.Silent -> { /* no-op */ }

                is VoiceResponse.Earcon -> earconPlayer.play(response.id)

                is VoiceResponse.Spoken -> ttsEngine.speak(response.text, language)

                is VoiceResponse.Combined -> {
                    earconPlayer.play(response.earcon)
                    ttsEngine.speak(response.spokenText, language)
                }
            }
        }
    }

    fun playEarcon(id: EarconId) {
        if (released) return
        earconPlayer.play(id)
    }

    fun release() {
        released = true
        scope.cancel()
        earconPlayer.release()
        ttsEngine.release()
    }
}
