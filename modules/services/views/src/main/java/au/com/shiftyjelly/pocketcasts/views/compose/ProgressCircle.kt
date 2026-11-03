package au.com.shiftyjelly.pocketcasts.views.compose

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode

internal sealed interface ProgressCircleState {
    data object Full : ProgressCircleState

    data object Empty : ProgressCircleState

    data class Progress(val degrees: Float, val clockwise: Boolean) : ProgressCircleState

    companion object {
        fun forEpisode(episode: BaseEpisode, isPlayed: Boolean): ProgressCircleState = when {
            isPlayed -> Empty
            episode.isInProgress -> Progress(remainingDegrees(episode.playedUpToMs, episode.durationMs), clockwise = false)
            else -> Full
        }

        fun forPercent(percent: Float): ProgressCircleState = Progress(360f * percent, clockwise = true)

        private fun remainingDegrees(progressMs: Int, durationMs: Int): Float {
            return if (durationMs <= 0 || progressMs < 0) 360f else (360.0 - progressMs.toDouble() / durationMs.toDouble() * 360.0).toFloat()
        }
    }
}

@Composable
internal fun ProgressCircle(
    state: ProgressCircleState,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Spacer(
        modifier.drawWithCache {
            val strokeWidth = 2.dp.toPx()
            val halfSize = (size.width.toInt() / 2).toFloat()
            val radius = halfSize - strokeWidth / 2
            val rectEnd = size.width.toInt() - strokeWidth / 2
            val arcRect = RectF(strokeWidth / 2, strokeWidth / 2, rectEnd, rectEnd)
            val paint = Paint().apply {
                strokeCap = Paint.Cap.BUTT
                style = Paint.Style.STROKE
                isAntiAlias = true
                this.strokeWidth = strokeWidth
                this.color = color.toArgb()
            }
            onDrawBehind {
                drawIntoCanvas { composeCanvas ->
                    val canvas = composeCanvas.nativeCanvas
                    when (state) {
                        ProgressCircleState.Empty -> Unit

                        ProgressCircleState.Full -> {
                            paint.alpha = 255
                            canvas.drawCircle(halfSize, halfSize, radius, paint)
                        }

                        is ProgressCircleState.Progress -> {
                            paint.alpha = TRACK_ALPHA
                            canvas.drawCircle(halfSize, halfSize, radius, paint)
                            if (state.degrees > 0) {
                                paint.alpha = 255
                                canvas.drawArc(arcRect, 270f, if (state.clockwise) state.degrees else -state.degrees, false, paint)
                            }
                        }
                    }
                }
            }
        },
    )
}

private const val TRACK_ALPHA = 80
