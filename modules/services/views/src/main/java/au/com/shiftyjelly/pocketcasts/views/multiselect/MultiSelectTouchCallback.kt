package au.com.shiftyjelly.pocketcasts.views.multiselect

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class MultiSelectTouchCallback(
    private val listener: ItemTouchHelperAdapter
) : ItemTouchHelper.Callback() {
    interface ItemTouchHelperAdapter {
        fun onItemMove(fromPosition: Int, toPosition: Int)
        fun onItemStartDrag(viewHolder: MultiSelectAdapter.ItemViewHolder)
        fun onItemTouchFinished(position: Int)
    }

    interface ItemTouchHelperViewHolder {
        fun onItemDrag()
        fun onItemSwipe()
        fun onItemClear()
    }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        if (viewHolder is MultiSelectAdapter.ItemViewHolder) {
            val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
            return ItemTouchHelper.SimpleCallback.makeMovementFlags(dragFlags, 0)
        } else {
            return ItemTouchHelper.SimpleCallback.makeMovementFlags(0, 0)
        }
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        if (viewHolder is MultiSelectAdapter.ItemViewHolder && target is MultiSelectAdapter.ItemViewHolder &&
            viewHolder.bindingAdapterPosition != RecyclerView.NO_POSITION && target.bindingAdapterPosition != RecyclerView.NO_POSITION
        ) {
            listener.onItemMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
            return true
        } else {
            return false
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
        listener.onItemTouchFinished(viewHolder.bindingAdapterPosition)
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return false
    }

    override fun isLongPressDragEnabled(): Boolean {
        return true
    }
}
