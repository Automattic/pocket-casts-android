package au.com.shiftyjelly.pocketcasts.voicecontrol.audio

interface VoiceAudioSegmenter {
    fun process(frame: PcmAudioFrame): VoiceSegmenterResult
}

sealed interface VoiceSegmenterResult {
    data object Silence : VoiceSegmenterResult
    data object SpeechStarted : VoiceSegmenterResult
    data object SpeechContinuing : VoiceSegmenterResult
    data class SpeechEnded(
        val frames: List<PcmAudioFrame>,
        val speechOnsetSample: Int = 0,
    ) : VoiceSegmenterResult
    data class Rejected(val reason: RejectionReason) : VoiceSegmenterResult
}

enum class RejectionReason {
    /**
     * Audio segment was too short to be considered valid speech
     */
    TooShort,

    /**
     * Audio signal was too weak or unclear
     */
    LowConfidence,

    /**
     * Audio processing timeout occurred
     */
    Timeout,

    /**
     * Audio route became invalid during capture
     */
    InvalidRoute,
}
