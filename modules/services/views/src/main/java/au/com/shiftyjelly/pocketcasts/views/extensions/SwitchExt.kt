package au.com.shiftyjelly.pocketcasts.views.extensions

import android.R
import android.content.res.ColorStateList
import android.os.Build
import android.widget.Switch
import androidx.annotation.ColorInt
import androidx.appcompat.widget.SwitchCompat
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils

fun Switch.updateTint(@ColorInt tintColor: Int, @ColorInt disabledColor: Int) {
    // Dynamic tinting of switches only supported on 23+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val thumbColorList = ColorStateList(
            arrayOf(
                intArrayOf(R.attr.state_checked), // Enabled
                intArrayOf()
            ),
            intArrayOf(
                tintColor,
                disabledColor
            )
        )

        thumbTintList = thumbColorList

        val trackColorList = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(android.R.attr.state_checked), // Enabled
                intArrayOf()
            ),
            intArrayOf(
                tintColor,
                tintColor,
                ColorUtils.colorWithAlpha(tintColor, 128)
            )
        )
        trackTintList = trackColorList
    }
}

fun SwitchCompat.updateTint(@ColorInt tintColor: Int, @ColorInt disabledColor: Int) {
    // Dynamic tinting of switches only supported on 23+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val thumbColorList = ColorStateList(
            arrayOf(
                intArrayOf(R.attr.state_checked), // Enabled
                intArrayOf()
            ),
            intArrayOf(
                tintColor,
                disabledColor
            )
        )

        thumbTintList = thumbColorList

        val trackColorList = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(android.R.attr.state_checked), // Enabled
                intArrayOf()
            ),
            intArrayOf(
                tintColor,
                tintColor,
                ColorUtils.colorWithAlpha(tintColor, 128)
            )
        )
        trackTintList = trackColorList
    }
}
