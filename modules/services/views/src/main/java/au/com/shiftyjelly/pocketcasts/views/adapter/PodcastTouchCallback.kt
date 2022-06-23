package au.com.shiftyjelly.pocketcasts.views.adapter

import android.content.Context
import androidx.recyclerview.widget.ItemTouchHelper

class PodcastTouchCallback(val adapter: ItemTouchHelperAdapter, val context: Context) : ItemTouchHelper.Callback() {

    private var isMoving = false

    interface ItemTouchHelperAdapter {
        fun onPodcastMove(fromPosition: Int, toPosition: Int)
        fun onPodcastMoveFinished()
    }

    interface ItemTouchHelperViewHolder {
        fun onItemDrag()
        fun onItemClear()
    }

    override fun getMovementFlags(recyclerView: androidx.recyclerview.widget.RecyclerView, viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT or ItemTouchHelper.START or ItemTouchHelper.END
        val swipeFlags = 0
        return ItemTouchHelper.SimpleCallback.makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(recyclerView: androidx.recyclerview.widget.RecyclerView, viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, target: androidx.recyclerview.widget.RecyclerView.ViewHolder): Boolean {
        adapter.onPodcastMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
        isMoving = true
        return true
    }

    override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
    }

    override fun onSelectedChanged(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder?, actionState: Int) {
        if (viewHolder is ItemTouchHelperViewHolder) {
            when (actionState) {
                ItemTouchHelper.ACTION_STATE_DRAG -> viewHolder.onItemDrag()
            }
        }

        super.onSelectedChanged(viewHolder, actionState)
    }

    override fun clearView(recyclerView: androidx.recyclerview.widget.RecyclerView, viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        if (viewHolder is ItemTouchHelperViewHolder) {
            viewHolder.onItemClear()
        }

        if (isMoving) {
            adapter.onPodcastMoveFinished()
            isMoving = false
        }
    }

    override fun isLongPressDragEnabled(): Boolean {
        return true
    }
}
