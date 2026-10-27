package au.com.shiftyjelly.pocketcasts.views.multiselect

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

internal data class DragItemBounds(
    val key: Any,
    val top: Int,
    val bottom: Int,
) {
    val height get() = bottom - top
}

internal object ItemTouchDragRules {
    private const val MOVE_THRESHOLD = 0.5f
    private const val SCROLL_ACCELERATION_LIMIT_MS = 2000L

    fun dropTarget(selected: DragItemBounds, items: List<DragItemBounds>, draggedTop: Float): DragItemBounds? {
        val curY = draggedTop.toInt()
        if (abs(curY - selected.top) < selected.height * MOVE_THRESHOLD) {
            return null
        }
        val top = draggedTop.roundToInt()
        val bottom = top + selected.height
        val centerY = (top + bottom) / 2
        val targets = mutableListOf<DragItemBounds>()
        val distances = mutableListOf<Int>()
        items.forEach { other ->
            if (other.key == selected.key || other.bottom < top || other.top > bottom) {
                return@forEach
            }
            val dy = abs(centerY - (other.top + other.bottom) / 2)
            val distance = dy * dy
            val position = distances.indexOfFirst { distance <= it }.takeIf { it >= 0 } ?: distances.size
            targets.add(position, other)
            distances.add(position, distance)
        }
        return chooseDropTarget(selected, targets, curY)
    }

    private fun chooseDropTarget(selected: DragItemBounds, targets: List<DragItemBounds>, curY: Int): DragItemBounds? {
        val bottom = curY + selected.height
        val dy = curY - selected.top
        var winner: DragItemBounds? = null
        var winnerScore = -1
        targets.forEach { target ->
            if (dy < 0) {
                val diff = target.top - curY
                if (diff > 0 && target.top < selected.top && diff > winnerScore) {
                    winnerScore = diff
                    winner = target
                }
            }
            if (dy > 0) {
                val diff = target.bottom - bottom
                if (diff < 0 && target.bottom > selected.bottom && -diff > winnerScore) {
                    winnerScore = -diff
                    winner = target
                }
            }
        }
        return winner
    }

    fun outOfBounds(draggedTop: Float, dy: Float, height: Int, paddingTop: Int, viewportHeight: Int, paddingBottom: Int): Int {
        val curY = draggedTop.toInt()
        val topDiff = curY - paddingTop
        if (dy < 0 && topDiff < 0) {
            return topDiff
        }
        if (dy > 0) {
            val bottomDiff = curY + height - (viewportHeight - paddingBottom)
            if (bottomDiff > 0) {
                return bottomDiff
            }
        }
        return 0
    }

    fun outOfBoundsScroll(height: Int, outOfBounds: Int, msSinceStartScroll: Long, maxScroll: Int): Int {
        val direction = sign(outOfBounds.toFloat()).toInt()
        val outOfBoundsRatio = min(1f, 1f * abs(outOfBounds) / height)
        val cappedScroll = (direction * maxScroll * scrollCap(outOfBoundsRatio)).toInt()
        val timeRatio = if (msSinceStartScroll > SCROLL_ACCELERATION_LIMIT_MS) 1f else msSinceStartScroll.toFloat() / SCROLL_ACCELERATION_LIMIT_MS
        val value = (cappedScroll * pow5(timeRatio)).toInt()
        return if (value == 0) {
            if (outOfBounds > 0) 1 else -1
        } else {
            value
        }
    }

    private fun scrollCap(ratio: Float) = pow5(ratio - 1f) + 1f

    private fun pow5(value: Float) = value * value * value * value * value
}
