package au.com.shiftyjelly.pocketcasts.views.adapter

import android.content.Context
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

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

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT or ItemTouchHelper.START or ItemTouchHelper.END
        val swipeFlags = 0
        return ItemTouchHelper.SimpleCallback.makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        val position = viewHolder.bindingAdapterPosition
        val targetPosition = target.bindingAdapterPosition
        if (position == RecyclerView.NO_POSITION || targetPosition == RecyclerView.NO_POSITION) {
            return false
        }

        adapter.onPodcastMove(position, targetPosition)
        isMoving = true
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
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

        if (isMoving) {
            adapter.onPodcastMoveFinished()
            isMoving = false
        }
    }

    override fun isLongPressDragEnabled(): Boolean {
        return true
    }
}
