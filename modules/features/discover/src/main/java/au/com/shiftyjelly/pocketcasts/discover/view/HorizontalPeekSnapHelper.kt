package au.com.shiftyjelly.pocketcasts.discover.view

import android.view.View
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView

class HorizontalPeekSnapHelper(val padding: Int) : PagerSnapHelper() {
    var onSnapPositionChanged: ((Int) -> Unit)? = null

    override fun calculateDistanceToFinalSnap(layoutManager: RecyclerView.LayoutManager, targetView: View): IntArray? {
        val distanceToFinalSnap = super.calculateDistanceToFinalSnap(layoutManager, targetView)
            ?: return null

        distanceToFinalSnap[0] = distanceToFinalSnap[0] + padding
        return distanceToFinalSnap
    }

    override fun findTargetSnapPosition(layoutManager: RecyclerView.LayoutManager?, velocityX: Int, velocityY: Int): Int {
        val targetSnapPosition = super.findTargetSnapPosition(layoutManager, velocityX, velocityY)
        onSnapPositionChanged?.invoke(targetSnapPosition)
        return targetSnapPosition
    }
}
