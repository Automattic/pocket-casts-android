package au.com.shiftyjelly.pocketcasts.views.extensions

import android.util.DisplayMetrics
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import kotlin.math.absoluteValue

/**
 * Scrolls the adapter position to the top of the screen.
 * Unlike smoothScrollToPosition which just scrolls the position into view.
 */
fun RecyclerView.smoothScrollToTop(position: Int, offset: Int = 0) {
    val smoothScroller = object : LinearSmoothScroller(context) {
        override fun getVerticalSnapPreference(): Int {
            return LinearSmoothScroller.SNAP_TO_START
        }

        override fun calculateDyToMakeVisible(view: View, snapPreference: Int): Int {
            return super.calculateDyToMakeVisible(view, snapPreference) + offset
        }
    }
    smoothScroller.targetPosition = position
    this.layoutManager?.startSmoothScroll(smoothScroller)
}

fun RecyclerView.quickScrollToTop() {
    val smoothScroller = object : LinearSmoothScroller(context) {
        init {
            targetPosition = 0
        }

        override fun getVerticalSnapPreference() = LinearSmoothScroller.SNAP_TO_START

        override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
            val scrollOffset = computeVerticalScrollOffset()
            val scrollRange = computeVerticalScrollRange()

            return if (scrollRange > 0 && scrollOffset > 0) {
                val pixelsInRange = scrollRange * scrollOffset / scrollRange.toFloat()
                (MILLIS_PER_RANGE / pixelsInRange).coerceAtMost(MAX_MILLIS_PER_INCH / displayMetrics.densityDpi)
            } else {
                super.calculateSpeedPerPixel(displayMetrics)
            }
        }
    }

    layoutManager?.startSmoothScroll(smoothScroller)
}

fun RecyclerView.ViewHolder.hideRow() {
    itemView.isVisible = false
    itemView.layoutParams.height = 0
}

fun RecyclerView.ViewHolder.showRow() {
    itemView.isVisible = true
    itemView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
}

fun RecyclerView.hideKeyboardOnScroll() {
    val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    var totalDy = 0
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(
            recyclerView: RecyclerView,
            dx: Int,
            dy: Int,
        ) {
            if (recyclerView.scrollState != SCROLL_STATE_DRAGGING) {
                return
            }
            totalDy += dy.absoluteValue
            if (totalDy >= touchSlop) {
                totalDy = 0
                UiUtil.hideKeyboard(this@hideKeyboardOnScroll)
            }
        }
    })
}

private const val MILLIS_PER_RANGE = 1000f
private const val MAX_MILLIS_PER_INCH = 50f
