package au.com.shiftyjelly.pocketcasts.nowplaying

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun TvNowPlayingWaveform(
    isPlaying: Boolean,
    artworkSize: Dp,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.tvColors.textPrimary.copy(alpha = 0.8f)
    val envelope = remember { WaveformEnvelope() }
    var frameTimeNanos by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isPlaying) {
        val now = withFrameNanos { it }
        if (isPlaying) {
            envelope.fadeTo(1f, now)
        } else if (envelope.valueAt(now) > 0f) {
            envelope.fadeTo(0f, now)
        }
        while (isPlaying || !envelope.isSettledAt(frameTimeNanos)) {
            frameTimeNanos = withInfiniteAnimationFrameNanos { it }
        }
    }

    val windowWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    val artworkSizePx = with(LocalDensity.current) { artworkSize.toPx() }
    Spacer(
        modifier = modifier
            .requiredWidth(windowWidth * WidthFraction)
            .requiredHeight(MaxBarHeight)
            .drawBehind { drawWaveform(color, frameTimeNanos, envelope, artworkSizePx) },
    )
}

private fun DrawScope.drawWaveform(
    color: Color,
    timeNanos: Long,
    envelope: WaveformEnvelope,
    artworkSizePx: Float,
) {
    val env = envelope.valueAt(timeNanos)
    if (env <= 0.001f) return
    val time = timeNanos / 1e9
    val barWidthPx = BarWidth.toPx()
    val spacingPx = BarSpacing.toPx()
    val stepPx = barWidthPx + spacingPx
    val sideWidth = (size.width - artworkSizePx) / 2
    val barsPerSide = (sideWidth / stepPx).toInt()
    if (barsPerSide <= 0) return
    for (side in 0 until 2) {
        for (i in 0 until barsPerSide) {
            val normalizedIndex = i.toFloat() / max(1, barsPerSide - 1)
            val distanceFactor = if (side == 0) normalizedIndex else 1f - normalizedIndex
            val sineWave = (sin(time * 2.5 + i * 0.4) * 0.5 + 0.5).toFloat()
            val barHeight = max(MinBarHeight.toPx(), MaxBarHeight.toPx() * distanceFactor * sineWave * env)
            val x = if (side == 0) {
                size.width / 2 - artworkSizePx / 2 - (barsPerSide - i) * stepPx
            } else {
                size.width / 2 + artworkSizePx / 2 + i * stepPx + spacingPx
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(x, (size.height - barHeight) / 2),
                size = Size(barWidthPx, barHeight),
                cornerRadius = CornerRadius(BarCornerRadius.toPx()),
                alpha = 0.8f * distanceFactor * env,
            )
        }
    }
}

internal class WaveformEnvelope(
    fadeDuration: Duration = 2.seconds,
    settleMargin: Duration = 100.milliseconds,
) {
    private val fadeDurationNanos = fadeDuration.inWholeNanoseconds
    private val settleNanos = fadeDurationNanos + settleMargin.inWholeNanoseconds
    private var fromValue = 0f
    private var toValue = 0f
    private var startTimeNanos = 0L

    fun fadeTo(target: Float, timeNanos: Long) {
        fromValue = valueAt(timeNanos)
        toValue = target
        startTimeNanos = timeNanos
    }

    fun valueAt(timeNanos: Long): Float {
        val t = ((timeNanos - startTimeNanos).toDouble() / fadeDurationNanos).coerceIn(0.0, 1.0)
        val eased = if (t < 0.5) 2 * t * t else 1 - (-2 * t + 2).pow(2) / 2
        return (fromValue + (toValue - fromValue) * eased).toFloat()
    }

    fun isSettledAt(timeNanos: Long): Boolean {
        return fromValue == toValue || timeNanos - startTimeNanos >= settleNanos
    }
}

private val WidthFraction = 0.75f
private val BarWidth = 3.33.dp
private val BarSpacing = 4.67.dp
private val MaxBarHeight = 66.67.dp
private val MinBarHeight = 1.33.dp
private val BarCornerRadius = 1.33.dp
