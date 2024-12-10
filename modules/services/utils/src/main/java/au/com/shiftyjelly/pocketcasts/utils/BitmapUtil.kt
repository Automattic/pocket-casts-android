package au.com.shiftyjelly.pocketcasts.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import androidx.annotation.ColorInt
import androidx.core.graphics.applyCanvas
import kotlin.math.abs
import kotlin.math.roundToInt

fun Bitmap.fitToAspectRatio(
    aspectRatio: Float,
    @ColorInt backgroundColor: Int = Color.BLACK,
    bitmapConfig: Bitmap.Config = Bitmap.Config.ARGB_8888,
): Bitmap {
    val (newWidth, newHeight) = calculateNearestSize(width, height, aspectRatio)
    return Bitmap.createBitmap(newWidth, newHeight, bitmapConfig).applyCanvas {
        val paint = Paint().apply {
            isDither = true
            color = backgroundColor
        }
        drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        drawBitmap(
            this@fitToAspectRatio,
            abs(width - this@fitToAspectRatio.width).toFloat() / 2,
            abs(height - this@fitToAspectRatio.height).toFloat() / 2,
            null,
        )
    }
}

internal fun calculateNearestSize(
    width: Int,
    height: Int,
    aspectRatio: Float,
): Pair<Int, Int> {
    check(aspectRatio > 0) { "Aspect ratio must be a positive number: $aspectRatio" }
    return when (width.toFloat() / height) {
        aspectRatio -> width to height
        in 0f..aspectRatio -> (height * aspectRatio).roundToInt() to height
        else -> width to (width / aspectRatio).roundToInt()
    }
}
