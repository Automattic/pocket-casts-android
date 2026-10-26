package au.com.shiftyjelly.pocketcasts.compose.swipe

import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class SwipeRowState internal constructor(
    internal val draggableState: AnchoredDraggableState<SwipeRowAnchor>,
    private val scope: CoroutineScope,
) {
    internal var controller: SwipeRowController? = null

    val isOpen get() = controller?.isOpen ?: (draggableState.targetValue != SwipeRowAnchor.Resting)

    fun settle() {
        val controller = controller
        if (controller != null) {
            controller.settle()
        } else {
            scope.launch { settleAndWait() }
        }
    }

    internal suspend fun settleAndWait() {
        draggableState.animateTo(SwipeRowAnchor.Resting)
    }
}

@Composable
fun rememberSwipeRowState(key: Any? = null): SwipeRowState {
    val scope = rememberCoroutineScope()
    return remember(scope, key) {
        SwipeRowState(
            draggableState = AnchoredDraggableState(initialValue = SwipeRowAnchor.Resting),
            scope = scope,
        )
    }
}
