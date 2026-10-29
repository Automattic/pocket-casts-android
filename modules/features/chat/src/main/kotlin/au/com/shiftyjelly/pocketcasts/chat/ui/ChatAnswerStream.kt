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
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatMessage
import kotlin.math.roundToInt

@Stable
internal class ChatAnswerStream(
    private val progress: Animatable<Float, AnimationVector1D>,
    private val tickOffsets: Map<String, Int>,
    totalTicks: Int,
) {
    private val target = totalTicks.toFloat()

    private val startedUuids by derivedStateOf {
        tickOffsets.filterValues { offset -> offset == 0 || progress.value > offset }.keys
    }

    val isComplete by derivedStateOf { progress.value >= target }

    fun isStreaming(uuid: String) = uuid in tickOffsets && !isComplete

    fun hasStarted(uuid: String) = uuid !in tickOffsets || uuid in startedUuids

    private val revealProgressByUuid: Map<String, () -> Float> = tickOffsets.mapValues { (_, offset) ->
        { progress.value - offset }
    }

    fun revealProgress(uuid: String): (() -> Float)? = revealProgressByUuid[uuid]

    suspend fun play(isReduceMotionEnabled: Boolean) {
        val remainingTicks = target - progress.value
        when {
            remainingTicks <= 0f -> Unit

            isReduceMotionEnabled -> progress.snapTo(target)

            else -> progress.animateTo(
                target,
                tween(
                    durationMillis = revealDurationMillis(remainingTicks),
                    delayMillis = if (progress.value == 0f) REVEAL_START_DELAY_MS else 0,
                    easing = LinearEasing,
                ),
            )
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
    val tickCounts = remember(answer) { answer.map { message -> message.uuid to message.revealTickCount() } }
    val tickOffsets = remember(tickCounts) { answerTickOffsets(tickCounts.map { (_, count) -> count }) }
    val totalTicks = answerTotalTicks(tickCounts.map { (_, count) -> count })
    val progress = remember(answerUuid) {
        Animatable(if (isReduceMotionEnabled) totalTicks.toFloat() else 0f)
    }
    val stream = remember(progress, tickCounts) {
        answerUuid?.let {
            ChatAnswerStream(
                progress = progress,
                tickOffsets = tickCounts.mapIndexed { index, (uuid, _) -> uuid to tickOffsets[index] }.toMap(),
                totalTicks = totalTicks,
            )
        }
    }
    LaunchedEffect(stream, isReduceMotionEnabled) {
        stream?.play(isReduceMotionEnabled)
    }
    return stream
}

internal val ChatMessage?.isAnswer get() = this is ChatMessage.Assistant || this is ChatMessage.Quote

internal fun ChatMessage.revealTickCount(): Int = when (this) {
    is ChatMessage.Assistant -> TextRevealTimeline(displayText.withHangingBullets(0.sp)).ticks
    is ChatMessage.Quote -> TextRevealTimeline(quoteText(displayText, timestampLabel)).ticks
    is ChatMessage.User -> 0
}

internal fun answerTickOffsets(tickCounts: List<Int>): List<Int> = tickCounts.runningFold(0) { offset, count -> offset + count + MESSAGE_PAUSE_TICKS }

internal fun answerTotalTicks(tickCounts: List<Int>): Int = if (tickCounts.isEmpty()) 0 else answerTickOffsets(tickCounts)[tickCounts.lastIndex] + tickCounts.last()

private fun revealDurationMillis(ticks: Float) = (ticks * REVEAL_MS_PER_TICK).roundToInt().coerceAtMost(REVEAL_MAX_DURATION_MS)

private const val REVEAL_MS_PER_TICK = 10
private const val REVEAL_MAX_DURATION_MS = 6_000
private const val REVEAL_START_DELAY_MS = 150
internal const val MESSAGE_PAUSE_TICKS = PARAGRAPH_PAUSE_TICKS
