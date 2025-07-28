package au.com.shiftyjelly.pocketcasts.compose.reorderable

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import sh.calvin.reorderable.ReorderableLazyCollectionState
import sh.calvin.reorderable.rememberReorderableLazyListState
import timber.log.Timber

// Author: Evtim - https://github.com/Calvin-LL/Reorderable/issues/88#issuecomment-3030356851
/**
 * Provides a drag-aware data source for use with Calvin-LL's reorderable components.
 *
 * Maintains a working order of [items] during drag, and invokes [onCommit] with the final
 * reordered list after drag completion. While dragging or commit is pending, the reordered
 * view is returned; otherwise, the original [items] list is returned.
 *
 * The reorderable state is always created, but drag handling is bypassed when [enabled] is false.
 *
 * @param T Item type.
 * @param K Stable key type for identifying items.
 * @param I Identity type used by the reorderable state (e.g., ItemPosition).
 * @param S Reorderable state type.
 * @param items Source list.
 * @param enabled Enables or disables drag handling.
 * @param itemKey Extracts a stable key from each item.
 * @param rememberState Factory for the reorderable state with an onMove handler.
 * @param getIndex Maps reorderable identity to index.
 * @param onCommit Called after drag ends with the final reordered list.
 *
 * @return Pair of the display list and the reorderable state.
 */
@Composable
fun <T : Any, K : Any, I : Any, S : ReorderableLazyCollectionState<I>> rememberReorderableDataSource(
    items: List<T>,
    itemKey: (T) -> K,
    rememberState: @Composable (onMove: CoroutineScope.(I, I) -> Unit) -> S,
    getIndex: (I) -> Int,
    onCommit: (List<T>) -> Unit,
    enabled: Boolean = true,
): Pair<List<T>, S> {
    // key -> element lookup for fast remapping
    var keyToItem by remember { mutableStateOf(mapOf<K, T>()) }

    // key order that mutates during drag
    var workingOrder by remember { mutableStateOf(listOf<K>()) }

    // Instantiate the reorderState
    val reorderState = rememberState { from, to ->
        // Reorder
        workingOrder = workingOrder.toMutableList().apply {
            val toIndex = getIndex(to)
            val fromIndex = getIndex(from)
            try {
                add(toIndex, removeAt(fromIndex))
            } catch (e: Throwable) {
                Timber.tag("ReorderableDataSource").i("onMove failed: $e")
            }
        }
    }

    if (!enabled) {
        return items to reorderState
    }

    // The data source state
    var sourceState by remember { mutableStateOf(ReorderableDataSourceState.Normal) }

    // Update the source state on drag start/stop
    val isDragging = reorderState.isAnyItemDragging
    LaunchedEffect(isDragging, onCommit) {
        // Update state based on dragging
        if (isDragging) {
            // Updater workingOrder to latest snapshot if not dragging
            if (sourceState == ReorderableDataSourceState.Normal) {
                workingOrder = items.map(itemKey)
                keyToItem = items.associateBy(itemKey)
            }
            // Update state
            sourceState = ReorderableDataSourceState.Dragging
        } else if (sourceState == ReorderableDataSourceState.Dragging) {
            // Commit
            onCommit(workingOrder.mapNotNull { keyToItem[it] })
            sourceState = ReorderableDataSourceState.PendingRefresh
        }
    }

    // Monitor items changes in order to switch to Normal. We do this
    // because after commit on drag stop, we don't want to emit the original items
    // before the commit had a chance to recompose us with the updated items. We do this
    // in order to avoid flicker caused by transient resetting to the original unordered items
    // before the commited changes come through.
    //
    // Note that in case the commit did not update the items, we will stay in PendingRefresh
    // state until down the line either items changes, or another drag is started
    LaunchedEffect(items) {
        // Are we waiting a refresh?
        if (sourceState == ReorderableDataSourceState.PendingRefresh) {
            // items have changes, switch the state back to normal
            sourceState = ReorderableDataSourceState.Normal
        }
    }

    // Keep the original items in an updated state
    val originalItems by rememberUpdatedState(items)

    // Derive display items from all of the data
    val display by remember {
        derivedStateOf {
            if (sourceState == ReorderableDataSourceState.Normal) {
                originalItems
            } else {
                workingOrder.mapNotNull { keyToItem[it] }
            }
        }
    }

    return display to reorderState
}

@Composable
fun <T : Any, K : Any> rememberReorderableLazyListDataSource(
    listState: LazyListState,
    items: List<T>,
    itemKey: (T) -> K,
    onCommit: (List<T>) -> Unit,
    enabled: Boolean = true,
    onMove: (() -> Unit)? = null,
) = rememberReorderableDataSource(
    items = items,
    itemKey = itemKey,
    rememberState = { onMoveDelegate ->
        rememberReorderableLazyListState(listState) { from, to ->
            onMoveDelegate(from, to)
            onMove?.invoke()
        }
    },
    getIndex = LazyListItemInfo::index,
    onCommit = onCommit,
    enabled = enabled,
)

private enum class ReorderableDataSourceState {
    Normal,
    Dragging,
    PendingRefresh,
}
