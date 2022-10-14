package au.com.shiftyjelly.pocketcasts.player.view

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION

class ShelfTouchCallback(
    private val listener: ItemTouchHelperAdapter
) : ItemTouchHelper.Callback() {
    interface ItemTouchHelperAdapter {
        fun onShelfItemMove(fromPosition: Int, toPosition: Int)
        fun onShelfItemStartDrag(viewHolder: ShelfAdapter.ItemViewHolder)
        fun onShelfItemTouchHelperFinished(position: Int)
    }

    interface ItemTouchHelperViewHolder {
        fun onItemDrag()
        fun onItemSwipe()
        fun onItemClear()
    }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        return if (viewHolder is ShelfAdapter.ItemViewHolder) {
            val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
            ItemTouchHelper.SimpleCallback.makeMovementFlags(dragFlags, 0)
        } else {
            ItemTouchHelper.SimpleCallback.makeMovementFlags(0, 0)
        }
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        return if (viewHolder is ShelfAdapter.ItemViewHolder && target is ShelfAdapter.ItemViewHolder &&
            viewHolder.bindingAdapterPosition != NO_POSITION && target.bindingAdapterPosition != NO_POSITION
        ) {
            listener.onShelfItemMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
            true
        } else {
            false
        }
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        if (viewHolder is ItemTouchHelperViewHolder) {
            when (actionState) {
                ItemTouchHelper.ACTION_STATE_DRAG -> viewHolder.onItemDrag()
                ItemTouchHelper.ACTION_STATE_SWIPE -> viewHolder.onItemSwipe()
            }
        }

        super.onSelectedChanged(viewHolder, actionState)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        if (viewHolder is ItemTouchHelperViewHolder) {
            viewHolder.onItemClear()
        }
        listener.onShelfItemTouchHelperFinished(viewHolder.bindingAdapterPosition)
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return false
    }

    override fun isLongPressDragEnabled(): Boolean {
        return true
    }
}
