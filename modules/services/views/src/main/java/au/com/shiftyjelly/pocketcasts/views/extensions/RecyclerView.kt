package au.com.shiftyjelly.pocketcasts.views.extensions

import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

/**
 * Scrolls the adapter position to the top of the screen.
 * Unlike smoothScrollToPosition which just scrolls the position into view.
 */
fun RecyclerView.smoothScrollToTop(position: Int) {
    val smoothScroller = object : LinearSmoothScroller(context) {
        override fun getVerticalSnapPreference(): Int {
            return LinearSmoothScroller.SNAP_TO_START
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
                (MillisPerRange / pixelsInRange).coerceAtMost(MaxMillisPerInch / displayMetrics.densityDpi)
            } else {
                super.calculateSpeedPerPixel(displayMetrics)
            }
        }
    }

    layoutManager?.startSmoothScroll(smoothScroller)
}

private const val MillisPerRange = 1000f
private const val MaxMillisPerInch = 50f
