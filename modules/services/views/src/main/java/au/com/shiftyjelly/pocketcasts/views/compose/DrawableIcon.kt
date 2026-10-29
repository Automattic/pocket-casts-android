package au.com.shiftyjelly.pocketcasts.views.compose

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.createBitmap

@Composable
internal fun DrawableIcon(
    @DrawableRes drawableId: Int,
    tint: Color?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val drawable = remember(drawableId, context) { AppCompatResources.getDrawable(context, drawableId)?.mutate() }
    Spacer(
        modifier.drawBehind {
            if (drawable != null) {
                drawable.setTintList(tint?.let { ColorStateList.valueOf(it.toArgb()) })
                drawable.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                drawIntoCanvas { drawable.draw(it.nativeCanvas) }
            }
        },
    )
}

@Composable
internal fun GradientDrawableIcon(
    @DrawableRes drawableId: Int,
    startColor: Color,
    endColor: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val drawable = remember(drawableId, context) { AppCompatResources.getDrawable(context, drawableId)?.mutate() }
    Spacer(
        modifier.drawWithCache {
            val bitmap = drawable?.let { gradientBitmap(it, size.width.toInt(), size.height.toInt(), startColor.toArgb(), endColor.toArgb()) }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            onDrawBehind {
                if (bitmap != null) {
                    drawIntoCanvas { it.nativeCanvas.drawBitmap(bitmap, 0f, 0f, paint) }
                }
            }
        },
    )
}

private fun gradientBitmap(drawable: Drawable, width: Int, height: Int, startColor: Int, endColor: Int): Bitmap? {
    if (width <= 0 || height <= 0) return null
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, width, height)
    drawable.draw(canvas)
    val gradientPaint = Paint().apply {
        shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), startColor, endColor, Shader.TileMode.CLAMP)
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gradientPaint)
    return bitmap
}
