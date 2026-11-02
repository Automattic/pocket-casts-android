package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import kotlin.time.Duration

sealed interface GeneratedChapterSeek {
    val playbackTime: Duration?
    val resolveDuration: Duration

    data class Resolved(
        override val playbackTime: Duration,
        val isCached: Boolean,
        override val resolveDuration: Duration = Duration.ZERO,
    ) : GeneratedChapterSeek

    data class Unresolved(
        val reason: String,
        val isAttempted: Boolean,
        override val resolveDuration: Duration = Duration.ZERO,
    ) : GeneratedChapterSeek {
        override val playbackTime: Duration? get() = null
    }
}
