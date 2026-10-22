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
    private val chunkOffsets: Map<String, Int>,
    totalChunks: Int,
) {
    private val target = revealedChunkTarget(totalChunks).toFloat()

    private val startedUuids by derivedStateOf {
        chunkOffsets.filterValues { offset -> offset == 0 || progress.value > offset }.keys
    }

    val isComplete by derivedStateOf { progress.value >= target }

    fun isStreaming(uuid: String) = uuid in chunkOffsets && !isComplete

    fun hasStarted(uuid: String) = uuid !in chunkOffsets || uuid in startedUuids

    private val revealProgressByUuid: Map<String, () -> Float> = chunkOffsets.mapValues { (_, offset) ->
        { progress.value - offset }
    }

    fun revealProgress(uuid: String): (() -> Float)? = revealProgressByUuid[uuid]

    suspend fun play(isReduceMotionEnabled: Boolean) {
        val remainingChunks = target - progress.value
        when {
            remainingChunks <= 0f -> Unit
            isReduceMotionEnabled -> progress.snapTo(target)
            else -> progress.animateTo(target, tween(durationMillis = revealDurationMillis(remainingChunks), easing = LinearEasing))
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
    val chunkCounts = remember(answer) { answer.map { message -> message.uuid to message.revealChunkCount() } }
    val totalChunks = chunkCounts.sumOf { (_, count) -> count }
    val progress = remember(answerUuid) {
        Animatable(if (isReduceMotionEnabled) revealedChunkTarget(totalChunks).toFloat() else 0f)
    }
    val stream = remember(progress, chunkCounts) {
        answerUuid?.let {
            val offsets = chunkCounts.runningFold(0) { offset, (_, count) -> offset + count }
            ChatAnswerStream(
                progress = progress,
                chunkOffsets = chunkCounts.mapIndexed { index, (uuid, _) -> uuid to offsets[index] }.toMap(),
                totalChunks = totalChunks,
            )
        }
    }
    LaunchedEffect(stream, isReduceMotionEnabled) {
        stream?.play(isReduceMotionEnabled)
    }
    return stream
}

internal val ChatMessage?.isAnswer get() = this is ChatMessage.Assistant || this is ChatMessage.Quote

internal fun ChatMessage.revealChunkCount(): Int = revealChunks(revealWordCount()).size

internal fun ChatMessage.revealWordCount(): Int = when (this) {
    is ChatMessage.Assistant -> wordRanges(displayText).size
    is ChatMessage.Quote -> wordRanges(displayText).size + wordRanges(quoteTimestampText(timestampLabel)).size
    is ChatMessage.User -> 0
}

private fun revealDurationMillis(chunks: Float) = (chunks * REVEAL_MS_PER_CHUNK).roundToInt().coerceAtMost(REVEAL_MAX_DURATION_MS)

private const val REVEAL_MS_PER_CHUNK = 120
private const val REVEAL_MAX_DURATION_MS = 4_000
