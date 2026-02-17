package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.material.CircularProgressIndicator
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import kotlin.math.min
import kotlinx.coroutines.launch

/**
 * Pull-to-refresh component for Wear OS that wraps content and shows a circular progress indicator
 * when the user pulls down at the top of the list.
 *
 * Behavior matches Gmail's Wear OS app:
 * - Only triggers when scrolled to the top
 * - Shows circular indicator that grows as user pulls
 * - Triggers refresh when pull threshold is reached
 * - Shows indeterminate progress during refresh
 *
 * @param state The refresh state from the ViewModel
 * @param listState The ScalingLazyListState to detect scroll position
 * @param onRefresh Callback to trigger refresh
 * @param modifier Modifier for the component
 * @param refreshThreshold The distance to pull before triggering refresh (default 80dp)
 * @param indicatorSize Size of the circular progress indicator (default 24dp)
 * @param content The content to display (typically a ScalingLazyColumn)
 */
@Composable
fun PullToRefresh(
    state: RefreshState,
    listState: ScalingLazyListState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    refreshThreshold: Dp = 80.dp,
    indicatorSize: Dp = 24.dp,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val refreshThresholdPx = with(density) { refreshThreshold.toPx() }
    val scope = rememberCoroutineScope()

    var pullOffset by remember { mutableFloatStateOf(0f) }
    var isRefreshing by remember { mutableStateOf(false) }
    var gestureStartedAtTop by remember { mutableStateOf(false) }
    val indicatorOffsetAnim = remember { Animatable(0f) }

    LaunchedEffect(state) {
        isRefreshing = state is RefreshState.Refreshing
        if (!isRefreshing) {
            pullOffset = 0f
            indicatorOffsetAnim.animateTo(0f, animationSpec = tween(300))
        }
    }

    val nestedScrollConnection = remember(listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (pullOffset > 0 && available.y < 0) {
                    val consumed = available.y.coerceAtLeast(-pullOffset)
                    pullOffset = (pullOffset + consumed).coerceAtLeast(0f)
                    scope.launch { indicatorOffsetAnim.snapTo(pullOffset) }
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (
                    available.y > 0 &&
                    !listState.canScrollBackward &&
                    gestureStartedAtTop &&
                    !isRefreshing
                ) {
                    val newOffset = (pullOffset + available.y).coerceAtMost(refreshThresholdPx * 1.5f)
                    pullOffset = newOffset
                    scope.launch { indicatorOffsetAnim.snapTo(newOffset) }
                    return Offset(0f, available.y)
                }

                if (source == NestedScrollSource.UserInput && available.y == 0f && consumed.y < 0f) {
                    gestureStartedAtTop = !listState.canScrollBackward
                }

                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                gestureStartedAtTop = !listState.canScrollBackward

                if (pullOffset >= refreshThresholdPx && !isRefreshing) {
                    onRefresh()
                    pullOffset = 0f
                    indicatorOffsetAnim.snapTo(refreshThresholdPx * 0.6f)
                } else if (pullOffset > 0) {
                    pullOffset = 0f
                    indicatorOffsetAnim.animateTo(0f, animationSpec = tween(200))
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                gestureStartedAtTop = !listState.canScrollBackward
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(listState.canScrollBackward) {
        if (!listState.canScrollBackward) {
            gestureStartedAtTop = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
    ) {
        content()

        if (isRefreshing || indicatorOffsetAnim.value > 0.1f) {
            val progress = min(1f, indicatorOffsetAnim.value / refreshThresholdPx)
            val alpha = if (isRefreshing) 1f else progress
            val indicatorTopDp = with(LocalDensity.current) {
                val maxTopPx = (indicatorSize + 8.dp).toPx()
                val topPx = (indicatorOffsetAnim.value / refreshThresholdPx) * maxTopPx
                topPx.toDp()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha),
                contentAlignment = Alignment.TopCenter,
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(indicatorSize)
                            .offset(y = indicatorSize + 8.dp),
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(indicatorSize)
                            .offset(y = indicatorTopDp),
                        progress = progress,
                    )
                }
            }
        }
    }
}
