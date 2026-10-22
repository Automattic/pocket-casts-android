package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatMessage
import kotlin.math.roundToInt

@Stable
internal class ChatAnswerStream(
    private val progress: Animatable<Float, AnimationVector1D>,
    private val wordOffsets: Map<String, Int>,
    totalWords: Int,
) {
    private val target = revealedWordTarget(totalWords).toFloat()

    private val startedUuids by derivedStateOf {
        wordOffsets.filterValues { offset -> offset == 0 || progress.value > offset }.keys
    }

    val isComplete by derivedStateOf { progress.value >= target }

    fun isStreaming(uuid: String) = uuid in wordOffsets && !isComplete

    fun hasStarted(uuid: String) = uuid !in wordOffsets || uuid in startedUuids

    fun wordProgress(uuid: String): (() -> Float)? {
        val offset = wordOffsets[uuid] ?: return null
        return { progress.value - offset }
    }

    suspend fun play(isReduceMotionEnabled: Boolean) {
        val remainingWords = target - progress.value
        when {
            remainingWords <= 0f -> Unit
            isReduceMotionEnabled -> progress.snapTo(target)
            else -> progress.animateTo(target, tween(durationMillis = revealDurationMillis(remainingWords), easing = LinearEasing))
        }
    }

    suspend fun skip() {
        progress.snapTo(target)
    }
}

@Composable
internal fun rememberChatAnswerStream(
    messages: List<ChatMessage>,
    initialMessageUuids: Set<String>,
): ChatAnswerStream? {
    val answer = messages.takeLastWhile { it.isAnswer }
    val answerUuid = answer.firstOrNull()?.uuid?.takeIf { answer.none { message -> message.uuid in initialMessageUuids } }
    val isReduceMotionEnabled = rememberIsReduceMotionEnabled()
    val wordCounts = remember(answer) { answer.map { message -> message.uuid to message.revealWordCount() } }
    val totalWords = wordCounts.sumOf { (_, count) -> count }
    val progress = remember(answerUuid) {
        Animatable(if (isReduceMotionEnabled) revealedWordTarget(totalWords).toFloat() else 0f)
    }
    val stream = remember(progress, wordCounts) {
        answerUuid?.let {
            val offsets = wordCounts.runningFold(0) { offset, (_, count) -> offset + count }
            ChatAnswerStream(
                progress = progress,
                wordOffsets = wordCounts.mapIndexed { index, (uuid, _) -> uuid to offsets[index] }.toMap(),
                totalWords = totalWords,
            )
        }
    }
    LaunchedEffect(stream) {
        stream?.play(isReduceMotionEnabled)
    }
    return stream
}

internal val ChatMessage?.isAnswer get() = this is ChatMessage.Assistant || this is ChatMessage.Quote

private fun ChatMessage.revealWordCount(): Int = when (this) {
    is ChatMessage.Assistant -> wordRanges(displayText).size
    is ChatMessage.Quote -> wordRanges(displayText).size + if (timestampLabel.isEmpty()) 0 else wordRanges("($timestampLabel)").size
    is ChatMessage.User -> 0
}

private fun revealDurationMillis(words: Float) = (words * REVEAL_MS_PER_WORD).roundToInt().coerceAtMost(REVEAL_MAX_DURATION_MS)

private const val REVEAL_MS_PER_WORD = 30
private const val REVEAL_MAX_DURATION_MS = 4_000
