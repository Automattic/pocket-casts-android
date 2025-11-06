package au.com.shiftyjelly.pocketcasts.views.adapter

import android.view.HapticFeedbackConstants
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class LockableListAdapter<T, VH : RecyclerView.ViewHolder>(
    callback: DiffUtil.ItemCallback<T>,
) : ListAdapter<T, VH>(callback) {
    internal var isLocked: Boolean = false

    internal fun forceSubmitList(list: List<T>?) {
        super.submitList(list)
    }

    override fun submitList(list: List<T>?) {
        if (isLocked) {
            return
        }
        super.submitList(list)
    }

    override fun submitList(list: List<T>?, commitCallback: Runnable?) {
        if (isLocked) {
            return
        }
        super.submitList(list, commitCallback)
    }

    interface DraggableHolder {
        fun onStartDragging()
        fun onFinishDragging()
    }
}

class LockingDragAndDropCallback<T>(
    private val scope: CoroutineScope,
    private val adapter: LockableListAdapter<T, *>,
    private val commitItems: suspend (List<T>) -> Unit,
) : ItemTouchHelper.Callback() {
    private var lastDraggedPosition = RecyclerView.NO_POSITION
    private var lastTargetPosition = RecyclerView.NO_POSITION
    private var reorderableItems: MutableList<T>? = null
    private var syncJob: Job? = null

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags = if (viewHolder is LockableListAdapter.DraggableHolder) {
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT or ItemTouchHelper.START or ItemTouchHelper.END
        } else {
            0
        }
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        if (viewHolder is LockableListAdapter.DraggableHolder && actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            syncJob?.cancel()
            adapter.isLocked = true
            reorderableItems = adapter.currentList.toMutableList()
            viewHolder.onStartDragging()
        }
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        if (target !is LockableListAdapter.DraggableHolder || viewHolder !is LockableListAdapter.DraggableHolder) {
            return false
        }

        val items = reorderableItems
        if (items == null) {
            return false
        }

        val draggedPosition = viewHolder.bindingAdapterPosition
        val targetPosition = target.bindingAdapterPosition
        if (draggedPosition == RecyclerView.NO_POSITION || targetPosition == RecyclerView.NO_POSITION) {
            return false
        }

        if (lastDraggedPosition == draggedPosition && lastTargetPosition == targetPosition) {
            return false
        }
        lastDraggedPosition = draggedPosition
        lastTargetPosition = targetPosition

        items.add(targetPosition, items.removeAt(draggedPosition))
        adapter.forceSubmitList(items.toList())
        viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)

        return true
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        if (viewHolder is LockableListAdapter.DraggableHolder) {
            viewHolder.onFinishDragging()
            syncJob?.cancel()
            syncJob = scope.launch {
                val items = reorderableItems
                if (items != null && lastDraggedPosition != RecyclerView.NO_POSITION && lastTargetPosition != RecyclerView.NO_POSITION) {
                    lastDraggedPosition = RecyclerView.NO_POSITION
                    lastTargetPosition = RecyclerView.NO_POSITION
                    commitItems(items)
                }
            }
            syncJob?.invokeOnCompletion { cause ->
                if (cause !is CancellationException) {
                    reorderableItems = null
                    adapter.isLocked = false
                }
            }
        }
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
}
