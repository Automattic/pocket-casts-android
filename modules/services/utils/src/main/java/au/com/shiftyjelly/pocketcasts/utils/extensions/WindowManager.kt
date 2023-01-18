package au.com.shiftyjelly.pocketcasts.utils.extensions

import android.graphics.Point
import android.os.Build
import android.view.WindowManager

fun WindowManager.deviceAspectRatio() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        (maximumWindowMetrics.bounds.height() / maximumWindowMetrics.bounds.width().toFloat())
    } else {
        val size = Point()
        @Suppress("DEPRECATION")
        defaultDisplay.getRealSize(size)
        size.y / size.x.toFloat()
    }
