package au.com.shiftyjelly.pocketcasts.player.view

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import kotlin.math.abs
import kotlin.math.sign

class UpNextTouchCallback(
    private val adapter: ItemTouchHelperAdapter
) : ItemTouchHelper.Callback() {

    interface ItemTouchHelperAdapter {
        fun onUpNextEpisodeMove(fromPosition: Int, toPosition: Int)
        fun onUpNextEpisodeRemove(position: Int)
        fun onUpNextEpisodeStartDrag(viewHolder: RecyclerView.ViewHolder)
        fun onUpNextItemTouchHelperFinished()
    }

    interface ItemTouchHelperViewHolder {
        fun onItemDrag()
        fun onItemClear()
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
    }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        if (viewHolder is UpNextEpisodeViewHolder) {
            val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
            return ItemTouchHelper.SimpleCallback.makeMovementFlags(dragFlags, 0)
        } else {
            return ItemTouchHelper.SimpleCallback.makeMovementFlags(0, 0)
        }
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        if (viewHolder is UpNextEpisodeViewHolder && target is UpNextEpisodeViewHolder &&
            viewHolder.bindingAdapterPosition != NO_POSITION && target.bindingAdapterPosition != NO_POSITION
        ) {
            adapter.onUpNextEpisodeMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
            return true
        } else {
            return false
        }
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        if (viewHolder is ItemTouchHelperViewHolder) {
            when (actionState) {
                ItemTouchHelper.ACTION_STATE_DRAG -> viewHolder.onItemDrag()
            }
        }

        super.onSelectedChanged(viewHolder, actionState)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        if (viewHolder is ItemTouchHelperViewHolder) {
            viewHolder.onItemClear()
        }

        viewHolder.setIsRecyclable(true)
        adapter.onUpNextItemTouchHelperFinished()
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return false
    }

    override fun isLongPressDragEnabled(): Boolean {
        return false
    }

    override fun interpolateOutOfBoundsScroll(recyclerView: RecyclerView, viewSize: Int, viewSizeOutOfBounds: Int, totalSize: Int, msSinceStartScroll: Long): Int {
        val minSpeed = SCROLL_MIN_SPEED_DP.dpToPx(recyclerView.context).toFloat()
        val maxSpeed = SCROLL_MAX_SPEED_DP.dpToPx(recyclerView.context).toFloat()

        val updatedViewSizeOutOfBounds = if (viewSizeOutOfBounds >= 0) {
            (viewSizeOutOfBounds - (recyclerView.paddingBottom * 0.7f).toInt()).coerceIn(1, viewSize)
        } else {
            (viewSizeOutOfBounds - (recyclerView.paddingTop * 0.7f).toInt()).coerceIn(-viewSize, -1)
        }
        val scrollTime = msSinceStartScroll.coerceIn(1000, SCROLL_FULL_SPEED_DELAY)
        var timeRatio = 1f
        if (scrollTime < SCROLL_FULL_SPEED_DELAY) {
            timeRatio = (scrollTime / SCROLL_FULL_SPEED_DELAY.toFloat())
        }
        val intention = updatedViewSizeOutOfBounds.toFloat() / viewSize
        var amount = ((intention * maxSpeed * timeRatio))
        amount = abs(amount).coerceIn(minSpeed, maxSpeed) * sign(amount)
        return amount.toInt()
    }
}

private const val SCROLL_MIN_SPEED_DP = 5
private const val SCROLL_MAX_SPEED_DP = 15
private const val SCROLL_FULL_SPEED_DELAY = 2400L
