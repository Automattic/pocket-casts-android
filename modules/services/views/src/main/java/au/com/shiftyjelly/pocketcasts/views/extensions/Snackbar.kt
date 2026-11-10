package au.com.shiftyjelly.pocketcasts.views.extensions

import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import com.google.android.material.snackbar.Snackbar

fun Snackbar.applyLightToastStyle(): Snackbar {
    setBackgroundTint(ThemeColor.primaryUi01(Theme.ThemeType.LIGHT))
    setTextColor(ThemeColor.primaryText01(Theme.ThemeType.LIGHT))
    view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        typeface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Typeface.create(Typeface.DEFAULT, 600, false)
        } else {
            Typeface.DEFAULT_BOLD
        }
    }
    val sideMargin = 16.dpToPx(context)
    view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        leftMargin = sideMargin
        rightMargin = sideMargin
    }
    return this
}
