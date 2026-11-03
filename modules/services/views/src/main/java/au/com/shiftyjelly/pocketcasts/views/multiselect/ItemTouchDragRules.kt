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

internal data class DragItemRect(
    val key: Any,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width get() = right - left
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

    fun gridDropTarget(selected: DragItemRect, items: List<DragItemRect>, draggedLeft: Float, draggedTop: Float): DragItemRect? {
        val curX = draggedLeft.toInt()
        val curY = draggedTop.toInt()
        if (abs(curY - selected.top) < selected.height * MOVE_THRESHOLD && abs(curX - selected.left) < selected.width * MOVE_THRESHOLD) {
            return null
        }
        val left = draggedLeft.roundToInt()
        val top = draggedTop.roundToInt()
        val right = left + selected.width
        val bottom = top + selected.height
        val centerX = (left + right) / 2
        val centerY = (top + bottom) / 2
        val targets = mutableListOf<DragItemRect>()
        val distances = mutableListOf<Int>()
        items.forEach { other ->
            if (other.key == selected.key || other.bottom < top || other.top > bottom || other.right < left || other.left > right) {
                return@forEach
            }
            val dx = abs(centerX - (other.left + other.right) / 2)
            val dy = abs(centerY - (other.top + other.bottom) / 2)
            val distance = dx * dx + dy * dy
            val position = distances.indexOfFirst { distance <= it }.takeIf { it >= 0 } ?: distances.size
            targets.add(position, other)
            distances.add(position, distance)
        }
        return chooseGridDropTarget(selected, targets, curX, curY)
    }

    private fun chooseGridDropTarget(selected: DragItemRect, targets: List<DragItemRect>, curX: Int, curY: Int): DragItemRect? {
        val right = curX + selected.width
        val bottom = curY + selected.height
        val dx = curX - selected.left
        val dy = curY - selected.top
        var winner: DragItemRect? = null
        var winnerScore = -1
        fun consider(target: DragItemRect, score: Int) {
            if (score > winnerScore) {
                winnerScore = score
                winner = target
            }
        }
        targets.forEach { target ->
            if (dx > 0 && target.right - right < 0 && target.right > selected.right) {
                consider(target, right - target.right)
            }
            if (dx < 0 && target.left - curX > 0 && target.left < selected.left) {
                consider(target, target.left - curX)
            }
            if (dy < 0 && target.top - curY > 0 && target.top < selected.top) {
                consider(target, target.top - curY)
            }
            if (dy > 0 && target.bottom - bottom < 0 && target.bottom > selected.bottom) {
                consider(target, bottom - target.bottom)
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
