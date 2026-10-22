package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.annotation.StringRes
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal enum class ChatLoadingPhase(
    val startsAfter: Duration,
    @StringRes val textRes: Int?,
) {
    Dots(Duration.ZERO, null),
    ReadingTranscript(1_200.milliseconds, LR.string.chat_loading_reading_transcript),
    FindingMoments(3_500.milliseconds, LR.string.chat_loading_finding_moments),
    WritingAnswer(6.seconds, LR.string.chat_loading_writing_answer),
    StillWorking(12.seconds, LR.string.chat_loading_still_working),
    ;

    val next: ChatLoadingPhase? get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun at(elapsed: Duration): ChatLoadingPhase = entries.lastOrNull { phase -> elapsed >= phase.startsAfter } ?: Dots
    }
}
