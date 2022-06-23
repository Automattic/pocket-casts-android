package au.com.shiftyjelly.pocketcasts.views.component

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx

private const val GAP_COUNT = 0
class RadioactiveLineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val lineHeight = 2.dpToPx(context)

    private val paint = Paint().apply {
        color = 0xFF119B00.toInt()
        alpha = 58
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null) return

        val gapWidth = width / (GAP_COUNT + 1).toFloat()
        (0..GAP_COUNT).forEach {
            val xPosition = (gapWidth * it)
            val rightPosition = xPosition + gapWidth
            (0..height step lineHeight * 2).forEach { position ->
                canvas.drawRect(xPosition + lineHeight / 2f, position.toFloat(), rightPosition - lineHeight / 2f, position.toFloat() + lineHeight, paint)
            }
        }
    }
}
