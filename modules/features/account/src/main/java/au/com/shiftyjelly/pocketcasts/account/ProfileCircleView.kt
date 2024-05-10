package au.com.shiftyjelly.pocketcasts.account

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toRect
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.extensions.getTintedDrawable
import coil.imageLoader
import coil.request.ImageRequest
import coil.target.Target
import coil.transform.CircleCropTransformation
import au.com.shiftyjelly.pocketcasts.ui.R as UR

private const val DRAW_FULL = 0
private const val DRAW_EMPTY = 1

class ProfileCircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var drawState = DRAW_EMPTY
    private var expiryDegrees = 0f
    private var plusAccount = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var color0 = ContextCompat.getColor(context, UR.color.plus_gold_dark)
    private var color1 = ContextCompat.getColor(context, UR.color.plus_gold_light)
    private var colorGrey = context.getThemeColor(UR.attr.primary_icon_02)

    private val inset = 2 * context.resources.displayMetrics.density
    private val strokeWidth = 2 * context.resources.displayMetrics.density
    private var outerCircleGradient: LinearGradient? = null
    private var innerCircleGradient: LinearGradient? = null
    private var bounds0Rect = RectF()
    private var bounds1Rect = RectF()
    private var bounds2Rect = RectF()
    private var bounds3Rect = RectF()
    private val iconColor = context.getThemeColor(UR.attr.primary_ui_01)
    private var iconDrawable0: Drawable? = context.getTintedDrawable(R.drawable.ic_plus_account, iconColor)
    private var iconDrawable1: Drawable? = context.getTintedDrawable(R.drawable.ic_free_account, iconColor)
    private var bitmap: Bitmap? = null
    private var isPatron: Boolean = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        update()
    }

    fun update() {
        val color0Res = if (isPatron) UR.color.patron_purple else UR.color.plus_gold_dark
        val color1Res = if (isPatron) UR.color.patron_purple else UR.color.plus_gold_light
        color0 = ContextCompat.getColor(context, color0Res)
        color1 = ContextCompat.getColor(context, color1Res)
        // -- outer circle
        val cx = width / 2f
        bounds0Rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        bounds0Rect.inset(inset, inset)
        outerCircleGradient = LinearGradient(cx, 0f, cx, height.toFloat(), color0, color1, Shader.TileMode.CLAMP)

        // -- inner circle
        bounds1Rect = RectF(bounds0Rect.left, bounds0Rect.top, bounds0Rect.right, bounds0Rect.bottom)
        bounds1Rect.inset(strokeWidth * 2, strokeWidth * 2)
        innerCircleGradient = LinearGradient(bounds1Rect.left, bounds1Rect.height() / 2, bounds1Rect.right, bounds1Rect.height() / 2, color1, color0, Shader.TileMode.CLAMP)

        bounds2Rect = RectF(bounds1Rect.left, bounds1Rect.top, bounds1Rect.right, bounds1Rect.bottom)
        if (!plusAccount) {
            val inset = width.toFloat() * 0.15f
            bounds3Rect = bounds2Rect
            bounds3Rect.inset(inset, inset)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (drawState == DRAW_EMPTY) {
            return
        } else if (drawState == DRAW_FULL) {
            // -- outer circle
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidth
            paint.shader = outerCircleGradient
            if (plusAccount) {
                canvas.drawArc(bounds0Rect, 270f, -expiryDegrees, false, paint)
            }

            // -- inner filled circle
            paint.style = Paint.Style.FILL
            if (plusAccount) {
                paint.shader = innerCircleGradient
            } else {
                paint.shader = null
                paint.color = colorGrey
            }
            canvas.drawCircle(bounds1Rect.centerX(), bounds1Rect.centerY(), bounds1Rect.width() / 2, paint)

            // -- inner icon
            if (plusAccount) {
                iconDrawable0?.bounds = bounds2Rect.toRect()
                iconDrawable0?.draw(canvas)
            } else {
                iconDrawable1?.bounds = bounds3Rect.toRect()
                iconDrawable1?.draw(canvas)
            }
            bitmap?.let {
                canvas.drawBitmap(
                    it,
                    bounds2Rect.centerX() - it.width / 2,
                    bounds2Rect.centerY() - it.height / 2,
                    paint,
                )
            }
        }
    }

    fun setup(
        percent: Float,
        plusOnly: Boolean,
        isPatron: Boolean = false,
        gravatarUrl: String? = null,
    ) {
        drawState = DRAW_FULL
        expiryDegrees = 360f * percent
        plusAccount = plusOnly
        bitmap = null
        gravatarUrl?.let {
            if (width > 0) {
                val size = if (!plusAccount) {
                    width * 0.85f
                } else {
                    width - (inset + strokeWidth * 2) * 2
                }
                loadImageFromUri(uri = Uri.parse(gravatarUrl), size = size.toInt())
            }
        }

        this.isPatron = isPatron
        val color0Res = if (isPatron) UR.attr.gradient_05_e else UR.attr.gradient_01_a
        val color1Res = if (isPatron) UR.attr.gradient_05_e else UR.attr.gradient_01_e
        color0 = context.getThemeColor(color0Res)
        color1 = context.getThemeColor(color1Res)
        update()
        invalidate()
    }

    private fun loadImageFromUri(uri: Uri, size: Int) {
        val target = object : Target {
            override fun onError(error: Drawable?) {
            }
            override fun onSuccess(result: Drawable) {
                bitmap = result.toBitmap()
                update()
                invalidate()
            }
        }
        val request = ImageRequest.Builder(context)
            .data(uri)
            .size(size)
            .transformations(CircleCropTransformation())
            .target(target)
        context.imageLoader.enqueue(request.build())
    }
}
