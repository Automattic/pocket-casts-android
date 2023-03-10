package au.com.shiftyjelly.pocketcasts.discover.util

import android.content.Context
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

/*
1. Increases scrolling speed of recyclerView.smoothScrollToPosition(position)
2. Sets custom extra layout space for pre caching an extra page
*/
class ScrollingLinearLayoutManager(
    context: Context?,
    orientation: Int,
    reverseLayout: Boolean,
) : LinearLayoutManager(context, orientation, reverseLayout) {
    override fun smoothScrollToPosition(
        recyclerView: RecyclerView,
        state: RecyclerView.State,
        position: Int,
    ) {
        val linearSmoothScroller: LinearSmoothScroller =
            object : LinearSmoothScroller(recyclerView.context) {
                override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                    return MILLISECONDS_PER_INCH / displayMetrics.densityDpi
                }
            }
        linearSmoothScroller.targetPosition = position
        startSmoothScroll(linearSmoothScroller)
    }

    /* By default, LinearLayoutManager lays out 1 extra page of items while smooth scrolling, in the direction of the scroll.
       This behavior is overridden to lay out an extra page even on a manual swipe. */
    override fun calculateExtraLayoutSpace(state: RecyclerView.State, extraLayoutSpace: IntArray) {
        extraLayoutSpace[0] = 0
        extraLayoutSpace[1] = this.width
    }

    companion object {
        private const val MILLISECONDS_PER_INCH = 100f // default is 25f (bigger = slower)
    }
}
