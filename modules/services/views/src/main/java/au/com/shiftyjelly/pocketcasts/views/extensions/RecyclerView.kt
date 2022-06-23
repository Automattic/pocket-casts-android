package au.com.shiftyjelly.pocketcasts.views.extensions

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
