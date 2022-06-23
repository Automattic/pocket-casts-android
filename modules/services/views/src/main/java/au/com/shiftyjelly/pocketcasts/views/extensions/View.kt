package au.com.shiftyjelly.pocketcasts.views.extensions

import android.graphics.Rect
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
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

fun View.toggleVisibility(): Boolean {
    this.visibility = if (this.visibility == View.GONE) View.VISIBLE else View.GONE
    return this.visibility == View.VISIBLE
}

fun View.findToolbar(): Toolbar {
    return findViewById(R.id.toolbar)
}

fun View.isVisible() = visibility == View.VISIBLE

fun View.isInvisible() = visibility == View.INVISIBLE

fun View.isHidden() = visibility == View.GONE

fun View.getPositionRectFromContainer(viewGroup: ViewGroup): Rect {
    val offsetViewBounds = Rect()
    this.getDrawingRect(offsetViewBounds)
    viewGroup.offsetDescendantRectToMyCoords(this, offsetViewBounds)
    return offsetViewBounds
}

fun View.setRippleBackground(borderless: Boolean = false) {
    val resId = if (borderless) android.R.attr.selectableItemBackgroundBorderless else android.R.attr.selectableItemBackground
    val outValue = TypedValue()
    context.theme.resolveAttribute(resId, outValue, true)
    this.setBackgroundResource(outValue.resourceId)
}

fun View.setRippleForeground(borderless: Boolean = false) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val resId = if (borderless) android.R.attr.selectableItemBackgroundBorderless else android.R.attr.selectableItemBackground
        val outValue = TypedValue()
        context.theme.resolveAttribute(resId, outValue, true)
        foreground = ContextCompat.getDrawable(context, outValue.resourceId)
    }
}

fun View.expand() {
    measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    val targtetHeight = measuredHeight

    layoutParams.height = 0
    visibility = View.VISIBLE
    val animation = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            layoutParams.height = if (interpolatedTime == 1f)
                ViewGroup.LayoutParams.WRAP_CONTENT
            else
                (targtetHeight * interpolatedTime).toInt()
            requestLayout()
        }

        override fun willChangeBounds(): Boolean {
            return true
        }
    }

    animation.duration = (targtetHeight / context.resources.displayMetrics.density).toInt().toLong()
    startAnimation(animation)
}

fun View.collapse() {
    val initialHeight = measuredHeight

    val animation = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            if (interpolatedTime == 1f) {
                visibility = View.GONE
            } else {
                layoutParams.height = initialHeight - (initialHeight * interpolatedTime).toInt()
                requestLayout()
            }
        }

        override fun willChangeBounds(): Boolean {
            return true
        }
    }

    animation.duration = (initialHeight / context.resources.displayMetrics.density).toInt().toLong()
    startAnimation(animation)
}
