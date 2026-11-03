package au.com.shiftyjelly.pocketcasts.views.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.views.multiselect.DragItemRect
import au.com.shiftyjelly.pocketcasts.views.multiselect.ItemTouchDragRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class GridDragState internal constructor(
    private val gridState: LazyGridState,
    private val scope: CoroutineScope,
    private val maxDragScroll: Int,
) {
    var itemInset = 0
    var startPadding = 0
    var isReorderable: (Any) -> Boolean = { false }
    var onStartDrag: () -> Unit = {}
    var onMove: (from: Any, to: Any) -> Unit = { _, _ -> }
    var onDrop: (Any) -> Unit = {}

    private var selectedKey by mutableStateOf<Any?>(null)
    private var recoveringKey by mutableStateOf<Any?>(null)
    private var liftedKey by mutableStateOf<Any?>(null)
    private var dx by mutableFloatStateOf(0f)
    private var dy by mutableFloatStateOf(0f)
    private var startLeft = 0
    private var startTop = 0
    private var initialTouch = Offset.Zero
    private var pendingMoveIndex: Int? = null
    private var scrollJob: Job? = null
    private var dragScrollStart = Long.MIN_VALUE
    private val recoverX = Animatable(0f)
    private val recoverY = Animatable(0f)

    fun isActive(key: Any) = key == selectedKey || key == recoveringKey

    fun isLifted(key: Any) = key == liftedKey

    fun translation(key: Any): Offset = when (key) {
        selectedKey -> {
            val rect = itemInfo(key)?.let(::rect)
            Offset(startLeft + dx - (rect?.left ?: startLeft), startTop + dy - (rect?.top ?: startTop))
        }

        recoveringKey -> Offset(recoverX.value, recoverY.value)

        else -> Offset.Zero
    }

    internal fun reorderableKeyAt(position: Offset): Any? = gridState.layoutInfo.visibleItemsInfo
        .lastOrNull { info ->
            val rect = rect(info)
            position.x >= rect.left && position.x <= rect.right && position.y >= rect.top && position.y <= rect.bottom
        }
        ?.key
        ?.takeIf(isReorderable)

    internal fun select(key: Any, touch: Offset): Boolean {
        if (recoveringKey != null) return false
        val rect = itemInfo(key)?.let(::rect) ?: return false
        pendingMoveIndex = null
        startLeft = rect.left
        startTop = rect.top
        initialTouch = touch
        dx = 0f
        dy = 0f
        selectedKey = key
        liftedKey = key
        onStartDrag()
        return true
    }

    internal fun onPointerMove(position: Offset) {
        dx = position.x - initialTouch.x
        dy = position.y - initialTouch.y
        moveIfNecessary()
        scrollJob?.cancel()
        scrollJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            while (scrollIfNecessary()) {
                moveIfNecessary()
                withFrameNanos { }
            }
        }
    }

    internal fun release() {
        val key = selectedKey ?: return
        val translation = translation(key)
        pendingMoveIndex = null
        scrollJob?.cancel()
        dragScrollStart = Long.MIN_VALUE
        selectedKey = null
        recoveringKey = key
        scope.launch {
            try {
                recoverX.snapTo(translation.x)
                recoverY.snapTo(translation.y)
                launch { recoverX.animateTo(0f, tween(250, easing = AccelerateDecelerate)) }
                recoverY.animateTo(0f, tween(250, easing = AccelerateDecelerate))
            } finally {
                recoveringKey = null
                liftedKey = null
                onDrop(key)
            }
        }
    }

    private fun moveIfNecessary() {
        val key = selectedKey ?: return
        val index = itemInfo(key)?.index ?: return
        if (index == pendingMoveIndex) return
        val rects = gridState.layoutInfo.visibleItemsInfo.map(::rect)
        val selected = rects.firstOrNull { it.key == key } ?: return
        val target = ItemTouchDragRules.gridDropTarget(selected, rects, startLeft + dx, startTop + dy) ?: return
        if (isReorderable(target.key)) {
            pendingMoveIndex = index
            onMove(key, target.key)
        }
    }

    private fun scrollIfNecessary(): Boolean {
        val rect = selectedKey?.let(::itemInfo)?.let(::rect)
        if (rect == null) {
            dragScrollStart = Long.MIN_VALUE
            return false
        }
        val now = System.currentTimeMillis()
        val duration = if (dragScrollStart == Long.MIN_VALUE) 0 else now - dragScrollStart
        val layout = gridState.layoutInfo
        var scrollY = ItemTouchDragRules.outOfBounds(
            draggedTop = startTop + dy - itemInset,
            dy = dy,
            height = rect.height + itemInset * 2,
            paddingTop = layout.beforeContentPadding,
            viewportHeight = layout.viewportSize.height,
            paddingBottom = layout.afterContentPadding,
        )
        if (scrollY != 0) {
            scrollY = ItemTouchDragRules.outOfBoundsScroll(rect.height, scrollY, duration, maxDragScroll)
            if (dragScrollStart == Long.MIN_VALUE) {
                dragScrollStart = now
            }
            gridState.dispatchRawDelta(scrollY.toFloat())
            return true
        }
        dragScrollStart = Long.MIN_VALUE
        return false
    }

    private fun itemInfo(key: Any) = gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }

    private fun rect(info: LazyGridItemInfo): DragItemRect {
        val left = info.offset.x + startPadding + itemInset
        val top = info.offset.y - gridState.layoutInfo.viewportStartOffset + itemInset
        return DragItemRect(info.key, left, top, left + info.size.width - itemInset * 2, top + info.size.height - itemInset * 2)
    }
}

@Composable
fun rememberGridDragState(gridState: LazyGridState): GridDragState {
    val scope = rememberCoroutineScope()
    val maxDragScroll = with(LocalDensity.current) { 20.dp.roundToPx() }
    return remember(gridState, scope, maxDragScroll) { GridDragState(gridState, scope, maxDragScroll) }
}

fun Modifier.gridDragGestures(state: GridDragState) = pointerInput(state) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (awaitLongPressOrCancellation(down.id) == null) return@awaitEachGesture
        val key = state.reorderableKeyAt(down.position) ?: return@awaitEachGesture
        if (!state.select(key, down.position)) return@awaitEachGesture
        var isFirstMove = true
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id }
            if (change == null || !change.pressed) break
            change.consume()
            if (event.type == PointerEventType.Move) {
                if (isFirstMove) isFirstMove = false else state.onPointerMove(change.position)
            }
        }
        state.release()
    }
}
