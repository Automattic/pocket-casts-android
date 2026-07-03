package au.com.shiftyjelly.pocketcasts.voicecontrol.intent

import au.com.shiftyjelly.pocketcasts.voicecontrol.feedback.EarconId

sealed interface VoiceResponse {
    data object Silent : VoiceResponse
    data class Earcon(val id: EarconId) : VoiceResponse
    data class Spoken(val text: String) : VoiceResponse
    data class Combined(val earcon: EarconId, val spokenText: String) : VoiceResponse
}
