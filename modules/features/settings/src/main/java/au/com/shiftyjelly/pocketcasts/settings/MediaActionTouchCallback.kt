package au.com.shiftyjelly.pocketcasts.settings

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class MediaActionTouchCallback(
    private val listener: ItemTouchHelperAdapter
) : ItemTouchHelper.Callback() {
    interface ItemTouchHelperAdapter {
        fun onMediaActionItemMove(fromPosition: Int, toPosition: Int)
        fun onMediaActionItemStartDrag(viewHolder: MediaActionAdapter.ItemViewHolder)
        fun onMediaActionItemTouchHelperFinished(position: Int)
    }

    interface ItemTouchHelperViewHolder {
        fun onItemDrag()
        fun onItemSwipe()
        fun onItemClear()
    }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        return if (viewHolder is MediaActionAdapter.ItemViewHolder) {
            val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
            ItemTouchHelper.SimpleCallback.makeMovementFlags(dragFlags, 0)
        } else {
            ItemTouchHelper.SimpleCallback.makeMovementFlags(0, 0)
        }
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        return if (viewHolder is MediaActionAdapter.ItemViewHolder && target is MediaActionAdapter.ItemViewHolder &&
            viewHolder.bindingAdapterPosition != RecyclerView.NO_POSITION && target.bindingAdapterPosition != RecyclerView.NO_POSITION
        ) {
            listener.onMediaActionItemMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
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
        listener.onMediaActionItemTouchHelperFinished(viewHolder.bindingAdapterPosition)
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return false
    }

    override fun isLongPressDragEnabled(): Boolean {
        return true
    }
}
