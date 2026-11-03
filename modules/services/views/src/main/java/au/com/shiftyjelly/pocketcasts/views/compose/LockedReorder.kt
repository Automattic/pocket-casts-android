package au.com.shiftyjelly.pocketcasts.views.compose

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LockedReorder<T> internal constructor(
    private val scope: CoroutineScope,
    private val view: View,
    private val keyOf: (T) -> Any,
    private val commitItems: suspend (List<T>) -> Unit,
) {
    internal var latestItems: List<T> = emptyList()
    private var lockedItems by mutableStateOf<List<T>?>(null)
    private var lastMove: Pair<Int, Int>? = null
    private var syncJob: Job? = null

    val items get() = lockedItems ?: latestItems

    fun bind(state: GridDragState) {
        state.isReorderable = { key -> items.any { keyOf(it) == key } }
        state.onStartDrag = {
            syncJob?.cancel()
            lockedItems = items
        }
        state.onMove = ::move
        state.onDrop = { drop() }
    }

    private fun move(fromKey: Any, toKey: Any) {
        val current = lockedItems ?: return
        val from = current.indexOfFirst { keyOf(it) == fromKey }
        val to = current.indexOfFirst { keyOf(it) == toKey }
        if (from < 0 || to < 0 || lastMove == from to to) return
        lastMove = from to to
        lockedItems = current.toMutableList().apply { add(to, removeAt(from)) }
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    private fun drop() {
        syncJob?.cancel()
        val job = scope.launch {
            val items = lockedItems
            if (items != null && lastMove != null) {
                lastMove = null
                commitItems(items)
            }
        }
        job.invokeOnCompletion { cause ->
            if (cause !is CancellationException) {
                lockedItems = null
            }
        }
        syncJob = job
    }
}

@Composable
fun <T> rememberLockedReorder(items: List<T>, keyOf: (T) -> Any, onCommit: suspend (List<T>) -> Unit): LockedReorder<T> {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val currentOnCommit by rememberUpdatedState(onCommit)
    val reorder = remember { LockedReorder(scope, view, keyOf) { currentOnCommit(it) } }
    reorder.latestItems = items
    return reorder
}
