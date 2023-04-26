package au.com.shiftyjelly.pocketcasts.views.component

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode

private const val STROKE_WIDTH = 2
private const val DRAW_FULL = 0
private const val DRAW_PROGRESS = 1
private const val DRAW_EMPTY = 2

class ProgressCircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var drawState = DRAW_FULL
    private var playbackDegrees = 0f
    private var density = context.resources.displayMetrics.density
    private var strokeWidthDp: Float = STROKE_WIDTH * density
    private val circlePaint = Paint()
    private var halfSize: Float = 0f
    private var radius: Float = 0f
    private var circleRect = RectF()
    private var directionMultiplier = -1f

    init {
        circlePaint.apply {
            strokeCap = Paint.Cap.BUTT
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeWidth = strokeWidthDp
        }
    }

    private fun calculatePlaybackDegrees(progressMs: Int, durationMs: Int): Float {
        return if (durationMs <= 0 || progressMs < 0) 360f else (360.0 - progressMs.toDouble() / durationMs.toDouble() * 360.0).toFloat()
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)

        halfSize = (width / 2).toFloat()
        val stokeWidthHalf = strokeWidthDp / 2
        radius = halfSize - stokeWidthHalf
        val rectSize = width - stokeWidthHalf
        circleRect = RectF(stokeWidthHalf, stokeWidthHalf, rectSize, rectSize)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (drawState == DRAW_EMPTY) {
            return
        } else if (drawState == DRAW_FULL) {
            circlePaint.alpha = 255
            canvas.drawCircle(halfSize, halfSize, radius, circlePaint)
        } else if (drawState == DRAW_PROGRESS) {
            circlePaint.alpha = 80
            canvas.drawCircle(halfSize, halfSize, radius, circlePaint)
            if (playbackDegrees > 0) {
                circlePaint.alpha = 255
                canvas.drawArc(circleRect, 270f, playbackDegrees * directionMultiplier, false, circlePaint)
            }
        }
    }

    fun setEpisode(episode: BaseEpisode, isPlayed: Boolean) {
        drawState = when {
            isPlayed -> DRAW_EMPTY
            episode.isInProgress -> {
                directionMultiplier = -1f
                playbackDegrees = calculatePlaybackDegrees(episode.playedUpToMs, episode.durationMs)
                DRAW_PROGRESS
            }
            else -> DRAW_FULL
        }
    }

    fun setPercent(percent: Float) {
        drawState = DRAW_PROGRESS
        directionMultiplier = 1f
        playbackDegrees = 360f * percent
        invalidate()
    }

    fun setColor(@ColorInt color: Int) {
        circlePaint.color = color
        invalidate()
    }
}
