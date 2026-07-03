package au.com.shiftyjelly.pocketcasts.voicecontrol.model

import au.com.shiftyjelly.pocketcasts.voicecontrol.mode.ListeningMode
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.MicExposure

data class VoiceRecognitionContext(
    val listeningMode: ListeningMode,
    val micExposure: MicExposure,
)
