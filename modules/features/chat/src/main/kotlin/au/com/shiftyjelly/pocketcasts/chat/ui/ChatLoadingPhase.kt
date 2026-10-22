package au.com.shiftyjelly.pocketcasts.chat.ui

import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
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

@Composable
internal fun rememberChatLoadingPhase(isAwaitingReply: Boolean): ChatLoadingPhase {
    var awaitingSinceMs by rememberSaveable { mutableStateOf<Long?>(null) }
    var phase by rememberSaveable { mutableStateOf(ChatLoadingPhase.Dots) }
    LaunchedEffect(isAwaitingReply) {
        if (!isAwaitingReply) {
            awaitingSinceMs = null
            return@LaunchedEffect
        }
        val sinceMs = awaitingSinceMs ?: SystemClock.elapsedRealtime().also { awaitingSinceMs = it }
        while (true) {
            val elapsed = (SystemClock.elapsedRealtime() - sinceMs).milliseconds
            phase = ChatLoadingPhase.at(elapsed)
            val next = phase.next ?: break
            delay(next.startsAfter - elapsed)
        }
    }
    return if (isAwaitingReply && awaitingSinceMs == null) ChatLoadingPhase.Dots else phase
}
