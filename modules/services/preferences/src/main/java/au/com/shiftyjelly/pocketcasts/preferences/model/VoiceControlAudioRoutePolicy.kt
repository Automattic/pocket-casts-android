package au.com.shiftyjelly.pocketcasts.preferences.model

enum class VoiceControlAudioRoutePolicy(val value: String) {
    HeadsetOnly("headset_only"),
    SpeakerExperimental("speaker_experimental"),
    ;

    companion object {
        fun fromValue(value: String): VoiceControlAudioRoutePolicy {
            return entries.firstOrNull { it.value == value } ?: HeadsetOnly
        }
    }
}
