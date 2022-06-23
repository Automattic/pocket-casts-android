package au.com.shiftyjelly.pocketcasts.views.component

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.use
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@SuppressLint("Recycle") // Kotlin use() doesn't work with this warning
class GradientIcon @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var drawableIcon: Drawable? = null
    var overlay: Drawable? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var color0: Int = context.getThemeColor(UR.attr.gradient_01_a)
    private var color1: Int = context.getThemeColor(UR.attr.gradient_01_e)
    private var boundsRect: RectF? = null
    private var gradientBitmap: Bitmap? = null

    private val gradientStarts = listOf(UR.attr.gradient_01_a, UR.attr.gradient_02_a, UR.attr.gradient_03_a, UR.attr.gradient_04_a, UR.attr.gradient_05_a)
    private val gradientEnds = listOf(UR.attr.gradient_01_e, UR.attr.gradient_02_e, UR.attr.gradient_03_e, UR.attr.gradient_04_e, UR.attr.gradient_05_e)

    init {
        if (attrs != null) {
            context.obtainStyledAttributes(attrs, UR.styleable.GradientIcon).use {
                var drawable: Drawable? = null
                var overlay: Drawable? = null

                if (it.hasValue(UR.styleable.GradientIcon_src)) {
                    drawable = AppCompatResources.getDrawable(context, it.getResourceId(UR.styleable.GradientIcon_src, -1))
                }

                if (it.hasValue(UR.styleable.GradientIcon_src_overlay)) {
                    overlay = AppCompatResources.getDrawable(context, it.getResourceId(UR.styleable.GradientIcon_src_overlay, -1))
                }

                if (it.hasValue(UR.styleable.GradientIcon_gradient)) {
                    val gradientIndex = it.getInt(UR.styleable.GradientIcon_gradient, 0)
                    val start = gradientStarts.getOrElse(gradientIndex) { UR.attr.gradient_01_a }
                    val end = gradientEnds.getOrElse(gradientIndex) { UR.attr.gradient_01_e }
                    color0 = context.getThemeColor(start)
                    color1 = context.getThemeColor(end)
                }

                setup(drawable, overlay = overlay)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        boundsRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        renderGradientBitmap()
        invalidate()
    }

    private fun renderGradientBitmap() {
        val boundsRect = boundsRect ?: return
        val drawable = drawableIcon ?: return

        if (boundsRect.width() <= 0 || boundsRect.height() <= 0) return

        val bitmap = Bitmap.createBitmap(
            boundsRect.width().toInt(),
            boundsRect.height().toInt(),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        val w = canvas.width
        val h = canvas.height
        drawable.setBounds(0, 0, w, h)
        drawable.draw(canvas)

        val paint = Paint()
        val shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            color0, color1, Shader.TileMode.CLAMP
        )
        paint.shader = shader
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        overlay?.let { overlay ->
            overlay.setBounds(0, 0, w, h)
            overlay.draw(canvas)
        }
        gradientBitmap = bitmap
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        gradientBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, paint)
        }
    }

    fun setup(drawable: Drawable?, colorA: Int? = null, colorB: Int? = null, overlay: Drawable? = null) {
        if (drawable == null) return
        drawableIcon = drawable
        this.overlay = overlay

        if (colorA != null) {
            color0 = colorA
        }
        if (colorB != null) {
            color1 = colorB
        }
        renderGradientBitmap()
        invalidate()
    }
}
