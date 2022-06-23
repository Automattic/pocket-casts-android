package au.com.shiftyjelly.pocketcasts.ui.helper

import android.graphics.Color
import androidx.annotation.ColorInt

object ColorUtils {
    @ColorInt fun calculateCombinedColor(@ColorInt originalColor: Int, @ColorInt overlayColor: Int): Int {
        val r1 = Color.red(originalColor).coerceIn(0, 255)
        val g1 = Color.green(originalColor).coerceIn(0, 255)
        val b1 = Color.blue(originalColor).coerceIn(0, 255)

        val r2 = Color.red(overlayColor).coerceIn(0, 255)
        val g2 = Color.green(overlayColor).coerceIn(0, 255)
        val b2 = Color.blue(overlayColor).coerceIn(0, 255)
        val a2 = Color.alpha(overlayColor).coerceIn(0, 255).toFloat() / 255f

        val red = r1 * (1 - a2) + r2 * a2
        val green = g1 * (1 - a2) + g2 * a2
        val blue = b1 * (1 - a2) + b2 * a2
        val alpha = 255

        return Color.argb(alpha, red.toInt(), green.toInt(), blue.toInt())
    }

    @ColorInt fun colorWithAlpha(@ColorInt color: Int, alpha: Int): Int {
        val r1 = Color.red(color).coerceIn(0, 255)
        val g1 = Color.green(color).coerceIn(0, 255)
        val b1 = Color.blue(color).coerceIn(0, 255)

        return Color.argb(alpha.coerceIn(0, 255), r1, g1, b1)
    }

    fun colorIntToHexString(colorInt: Int): String {
        return String.format("#%06X", 0xFFFFFF and colorInt)
    }
}

fun Int.colorIntWithAlpha(alpha: Int): Int {
    return ColorUtils.colorWithAlpha(this, alpha)
}
