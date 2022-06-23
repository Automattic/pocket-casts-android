package au.com.shiftyjelly.pocketcasts.views.tour

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import au.com.shiftyjelly.pocketcasts.ui.helper.colorIntWithAlpha

private const val PROP_X = "x"
private const val PROP_Y = "y"
private const val PROP_WIDTH = "width"
private const val PROP_HEIGHT = "height"

@Suppress("NAME_SHADOWING")
internal class TourBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val backgroundPaint = Paint()
    private val cutoutPaint = Paint()

    private var animator: ValueAnimator? = null
    private var cutoutXHolder: PropertyValuesHolder? = null
    private var cutoutYHolder: PropertyValuesHolder? = null
    private var cutoutWidthHolder: PropertyValuesHolder? = null
    private var cutoutHeightHolder: PropertyValuesHolder? = null

    private var cutoutX: Float? = null
    private var cutoutY: Float? = null
    private var cutoutWidth: Float? = null
    private var cutoutHeight: Float? = null
    private var cutoutRect: RectF? = null

    init {
        backgroundPaint.color = Color.BLACK.colorIntWithAlpha(204)
        cutoutPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)

        setLayerType(LAYER_TYPE_SOFTWARE, null) // Cutouts seem to only work with software drawing
    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        val canvas = canvas ?: return

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        val rect = cutoutRect ?: return
        val height = cutoutHeight ?: return

        canvas.drawRoundRect(rect, height / 2f, height / 2f, cutoutPaint)
    }

    fun moveCutout(rectF: RectF) {
        cutoutXHolder = PropertyValuesHolder.ofFloat(PROP_X, cutoutX ?: rectF.left, rectF.left)
        cutoutYHolder = PropertyValuesHolder.ofFloat(PROP_Y, cutoutY ?: rectF.top, rectF.top)
        cutoutWidthHolder = PropertyValuesHolder.ofFloat(PROP_WIDTH, cutoutWidth ?: (rectF.right - rectF.left), rectF.right - rectF.left)
        cutoutHeightHolder = PropertyValuesHolder.ofFloat(PROP_HEIGHT, cutoutHeight ?: (rectF.bottom - rectF.top), rectF.bottom - rectF.top)

        animator = ValueAnimator().apply {
            setValues(cutoutXHolder, cutoutYHolder, cutoutWidthHolder, cutoutHeightHolder)
            duration = 350
            addUpdateListener {
                cutoutX = it.getAnimatedValue(PROP_X) as? Float
                cutoutY = it.getAnimatedValue(PROP_Y) as? Float
                cutoutWidth = it.getAnimatedValue(PROP_WIDTH) as? Float
                cutoutHeight = it.getAnimatedValue(PROP_HEIGHT) as? Float

                if ((cutoutWidth ?: 0f) < (cutoutHeight ?: 0f)) { // A cutout should never be less wide as it is high
                    val x = cutoutX ?: 0f
                    val width = cutoutWidth ?: 0f
                    val height = cutoutHeight ?: 0f
                    cutoutWidth = cutoutHeight
                    cutoutX = x - (height - width) / 2f
                }
                cutoutRect = RectF(cutoutX ?: 0f, cutoutY ?: 0f, (cutoutX ?: 0f) + (cutoutWidth ?: 0f), (cutoutY ?: 0f) + (cutoutHeight ?: 0f))

                invalidate()
            }
        }

        animator?.start()
    }

    fun hideCutout() {
        cutoutRect = null
        cutoutHeight = null

        invalidate()
    }
}
