package au.com.shiftyjelly.pocketcasts.views.extensions

import android.os.Build
import android.util.TypedValue
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import au.com.shiftyjelly.pocketcasts.views.R

fun View.showIf(show: Boolean) {
    if (show) {
        show()
    } else {
        hide()
    }
}

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.hide() {
    this.visibility = View.GONE
}

fun View.findToolbar(): Toolbar {
    return findViewById(R.id.toolbar)
}

fun View.isVisible() = visibility == View.VISIBLE

fun View.setRippleBackground(borderless: Boolean = false) {
    val resId = if (borderless) android.R.attr.selectableItemBackgroundBorderless else android.R.attr.selectableItemBackground
    val outValue = TypedValue()
    context.theme.resolveAttribute(resId, outValue, true)
    this.setBackgroundResource(outValue.resourceId)
}

fun View.setSystemWindowInsetToPadding(
    left: Boolean = false,
    top: Boolean = false,
    right: Boolean = false,
    bottom: Boolean = false,
    consumeInsets: Boolean = false,
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(
            WindowInsetsCompat.Type.systemBars() or
                WindowInsetsCompat.Type.displayCutout(),
        )

        view.updatePadding(
            left = if (left) insets.left else paddingLeft,
            top = if (top) insets.top else paddingTop,
            right = if (right) insets.right else paddingRight,
            bottom = if (bottom) insets.bottom else paddingBottom,
        )

        if (consumeInsets) {
            ViewCompat.onApplyWindowInsets(
                view,
                windowInsets.inset(
                    if (left) insets.left else 0,
                    if (top) insets.top else 0,
                    if (right) insets.right else 0,
                    if (bottom) insets.bottom else 0,
                ),
            )
        } else {
            windowInsets
        }
    }
}

fun View.setSystemWindowInsetToHeight(
    top: Boolean = false,
    bottom: Boolean = false,
    consumeInsets: Boolean = false,
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

        view.updateLayoutParams {
            height = when {
                top -> insets.top
                bottom -> insets.bottom
                else -> 0
            }
        }

        if (consumeInsets) {
            ViewCompat.onApplyWindowInsets(
                view,
                windowInsets.inset(
                    0,
                    if (top) insets.top else 0,
                    0,
                    if (bottom) insets.bottom else 0,
                ),
            )
        } else {
            windowInsets
        }
    }
}

fun View.announceAccessibility(message: CharSequence) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        stateDescription = message
    } else {
        @Suppress("DEPRECATION")
        announceForAccessibility(message)
    }
}
