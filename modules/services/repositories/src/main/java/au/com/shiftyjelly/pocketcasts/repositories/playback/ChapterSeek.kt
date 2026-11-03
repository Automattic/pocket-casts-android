package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.ChapterSeekResult
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.GeneratedChapterSeek
import kotlin.time.Duration

data class ChapterSeek(
    val chapter: Chapter,
    val position: Duration,
    val generated: GeneratedChapterSeek?,
) {
    val isGeneratedFallback get() = generated is GeneratedChapterSeek.Unresolved &&
        generated.isAttempted &&
        generated.reason != ChapterSeekResult.REASON_METERED_NETWORK
}
