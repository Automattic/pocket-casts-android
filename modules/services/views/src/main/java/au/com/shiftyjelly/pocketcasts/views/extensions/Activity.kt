package au.com.shiftyjelly.pocketcasts.views.extensions

import android.util.TypedValue
import androidx.fragment.app.FragmentActivity

fun FragmentActivity.getActionBarHeight(): Int {
    val typedValue = TypedValue()
    if (theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
        return TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
    }
    return 0
}

fun FragmentActivity.getStatusBarHeight(): Int {
    val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
}
