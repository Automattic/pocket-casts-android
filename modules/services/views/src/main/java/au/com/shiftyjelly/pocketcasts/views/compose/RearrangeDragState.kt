package au.com.shiftyjelly.pocketcasts.views.compose

import android.animation.ArgbEvaluator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import au.com.shiftyjelly.pocketcasts.views.multiselect.DragItemBounds
import au.com.shiftyjelly.pocketcasts.views.multiselect.ItemTouchDragRules
import kotlin.math.PI
import kotlin.math.cos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class RearrangeDragState(
    private val listState: LazyListState,
    private val scope: CoroutineScope,
    private val haptics: HapticFeedback,
    private val liftElevation: Float,
    private val maxDragScroll: Int,
) {
    var isReorderable: (Any) -> Boolean = { false }
    var onMove: (from: Any, to: Any) -> Unit = { _, _ -> }
    var onStartDrag: (Any) -> Unit = {}
    var onDrop: (Any) -> Unit = {}

    private var selectedKey by mutableStateOf<Any?>(null)
    private var recoveringKey by mutableStateOf<Any?>(null)
    private var liftedKey by mutableStateOf<Any?>(null)
    private var dy by mutableFloatStateOf(0f)
    private var startTop = 0
    private var initialTouchY = 0f
    private var handleDownKey: Any? = null
    private var pendingMoveIndex: Int? = null
    private var scrollJob: Job? = null
    private var dragScrollStart = Long.MIN_VALUE
    private val lift = Animatable(0f)
    private val recoverTranslation = Animatable(0f)

    fun isActive(key: Any) = key == selectedKey || key == recoveringKey

    fun isLifted(key: Any) = key == liftedKey

    fun translationY(key: Any): Float = when (key) {
        selectedKey -> startTop + dy - (itemInfo(key)?.let(::top) ?: startTop)
        recoveringKey -> recoverTranslation.value
        else -> 0f
    }

    fun elevation(key: Any) = if (key == liftedKey) 1f + liftElevation * lift.value else 0f

    fun background(key: Any, color: Color) = if (key == liftedKey) {
        Color(ArgbEvaluator().evaluate(lift.value, android.graphics.Color.TRANSPARENT, color.toArgb()) as Int)
    } else {
        Color.Transparent
    }

    fun onHandleDown(key: Any) {
        handleDownKey = key
    }

    internal fun takeHandleDown() = handleDownKey.also { handleDownKey = null }

    internal fun clearHandleDown() {
        handleDownKey = null
    }

    internal fun reorderableKeyAt(y: Float): Any? = listState.layoutInfo.visibleItemsInfo
        .lastOrNull { y >= top(it) && y <= top(it) + it.size }
        ?.key
        ?.takeIf(isReorderable)

    internal fun select(key: Any, touchY: Float, fromHandle: Boolean): Boolean {
        if (recoveringKey != null) return false
        val info = itemInfo(key) ?: return false
        pendingMoveIndex = null
        startTop = top(info)
        initialTouchY = touchY
        dy = 0f
        selectedKey = key
        liftedKey = key
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            lift.snapTo(0f)
            lift.animateTo(1f, tween(300, easing = AccelerateDecelerate))
        }
        if (fromHandle) {
            onStartDrag(key)
        }
        return true
    }

    internal fun onPointerMove(y: Float) {
        dy = y - initialTouchY
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
        val translation = translationY(key)
        pendingMoveIndex = null
        scrollJob?.cancel()
        dragScrollStart = Long.MIN_VALUE
        selectedKey = null
        recoveringKey = key
        scope.launch {
            recoverTranslation.snapTo(translation)
            recoverTranslation.animateTo(0f, tween(250, easing = AccelerateDecelerate))
            recoveringKey = null
            liftedKey = null
            lift.snapTo(0f)
            onDrop(key)
        }
    }

    private fun moveIfNecessary() {
        val layout = listState.layoutInfo
        val key = selectedKey ?: return
        val index = itemInfo(key)?.index ?: return
        if (index == pendingMoveIndex) return
        val bounds = layout.visibleItemsInfo.map { DragItemBounds(it.key, top(it), top(it) + it.size) }
        val selected = bounds.firstOrNull { it.key == key } ?: return
        val target = ItemTouchDragRules.dropTarget(selected, bounds, startTop + dy) ?: return
        if (isReorderable(target.key)) {
            pendingMoveIndex = index
            onMove(key, target.key)
        }
    }

    private fun scrollIfNecessary(): Boolean {
        val info = selectedKey?.let(::itemInfo)
        if (info == null) {
            dragScrollStart = Long.MIN_VALUE
            return false
        }
        val now = System.currentTimeMillis()
        val duration = if (dragScrollStart == Long.MIN_VALUE) 0 else now - dragScrollStart
        val layout = listState.layoutInfo
        var scrollY = ItemTouchDragRules.outOfBounds(startTop + dy, dy, info.size, layout.beforeContentPadding, layout.viewportSize.height, layout.afterContentPadding)
        if (scrollY != 0) {
            scrollY = ItemTouchDragRules.outOfBoundsScroll(info.size, scrollY, duration, maxDragScroll)
            if (dragScrollStart == Long.MIN_VALUE) {
                dragScrollStart = now
            }
            listState.dispatchRawDelta(scrollY.toFloat())
            return true
        }
        dragScrollStart = Long.MIN_VALUE
        return false
    }

    private fun itemInfo(key: Any) = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }

    private fun top(info: LazyListItemInfo) = info.offset - listState.layoutInfo.viewportStartOffset
}

internal val AccelerateDecelerate = Easing { (cos((it + 1) * PI) / 2 + 0.5).toFloat() }

internal fun Modifier.rearrangeDragGestures(state: RearrangeDragState) = pointerInput(state) {
    awaitEachGesture {
        try {
            val down = awaitFirstDown(requireUnconsumed = false)
            val handleKey = state.takeHandleDown()
            val key = if (handleKey != null) {
                handleKey
            } else {
                if (awaitLongPressOrCancellation(down.id) == null) return@awaitEachGesture
                state.reorderableKeyAt(down.position.y) ?: return@awaitEachGesture
            }
            if (!state.select(key, down.position.y, fromHandle = handleKey != null)) return@awaitEachGesture
            var isFirstMove = true
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null || !change.pressed) break
                change.consume()
                if (event.type == PointerEventType.Move) {
                    if (isFirstMove) isFirstMove = false else state.onPointerMove(change.position.y)
                }
            }
            state.release()
        } finally {
            state.clearHandleDown()
        }
    }
}
