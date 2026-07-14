package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import kotlin.time.Duration

sealed interface ChapterSeekResult {
    data class Resolved(val playbackTime: Duration, val usedPrior: Boolean) : ChapterSeekResult
    data class Unresolved(val reason: String) : ChapterSeekResult

    companion object {
        const val REASON_TIMEOUT = "timeout"
        const val REASON_NO_AUDIO_SOURCE = "no_audio_source"
        const val REASON_NO_REFERENCE = "no_reference"
        const val REASON_AUDIO_UNAVAILABLE = "audio_unavailable"
        const val REASON_NO_MATCH = "no_match"
    }
}
