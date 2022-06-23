package au.com.shiftyjelly.pocketcasts.player.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class ChapterProgressCircle @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var density = context.resources.displayMetrics.density
    private var strokeWidthDp: Float = 2.0f * density
    private val circlePaint = Paint()
    private var halfSize: Float = 0f
    private var radius: Float = 0f
    private var circleRect = RectF()
    private var degrees: Float = 0f

    var percent: Float = 0f
        set(value) {
            field = value
            degrees = 360f * value
            invalidate()
        }

    init {
        circlePaint.apply {
            strokeCap = Paint.Cap.BUTT
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeWidth = strokeWidthDp
            color = Color.argb(102, 255, 255, 255)
        }
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

        if (degrees > 0) {
            circlePaint.alpha = 102
            canvas.drawArc(circleRect, -90f, -degrees, false, circlePaint)
        }
    }
}
