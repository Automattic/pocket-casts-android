package au.com.shiftyjelly.pocketcasts.player.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import au.com.shiftyjelly.pocketcasts.ui.helper.colorIntWithAlpha
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx

class ChapterProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var progress: Float = 0.0f
        set(value) {
            field = value
            progressDrawRect = RectF(0f, 0f, backgroundDrawRect.right * progress, backgroundDrawRect.bottom)
            invalidate()
        }

    val cornerRadius = 8.dpToPx(context).toFloat()
    var backgroundDrawRect = RectF()
    var progressDrawRect = RectF()
    var clipPath = Path()

    var theme: Theme.ThemeType = Theme.ThemeType.DARK
        set(value) {
            field = value
            backgroundPaint.color = when {
                field.darkTheme -> ThemeColor.playerContrast06(field)
                field == Theme.ThemeType.LIGHT_CONTRAST -> ThemeColor.primaryUi05(field)
                else -> ThemeColor.primaryUi02(field)
            }
            progressPaint.color = when {
                field == Theme.ThemeType.ROSE -> ThemeColor.primaryIcon02Selected(field).colorIntWithAlpha(25)
                field.darkTheme -> ThemeColor.playerContrast06(field).colorIntWithAlpha(25)
                else -> Color.BLACK.colorIntWithAlpha(25)
            }
        }

    val backgroundPaint = Paint().apply {
        this.color = Color.argb(25, 255, 255, 255)
        this.style = Paint.Style.FILL
    }

    var progressPaint = Paint().apply {
        this.color = Color.argb(50, 255, 255, 255)
        this.style = Paint.Style.FILL
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        backgroundDrawRect = RectF(0f, 0f, w.toFloat(), h.toFloat())
        progressDrawRect = RectF(0f, 0f, w * progress, h.toFloat())
        clipPath = Path().apply { addRoundRect(backgroundDrawRect, cornerRadius, cornerRadius, Path.Direction.CW) }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.clipPath(clipPath)
        canvas.drawRect(backgroundDrawRect, backgroundPaint)
        canvas.drawRect(progressDrawRect, progressPaint)
    }
}
