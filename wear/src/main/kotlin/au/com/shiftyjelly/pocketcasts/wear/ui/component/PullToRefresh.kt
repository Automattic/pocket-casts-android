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
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlinx.coroutines.launch
import timber.log.Timber

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

    // Track the current pull offset
    var pullOffset by remember { mutableFloatStateOf(0f) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Animation for the indicator offset
    val indicatorOffsetAnim = remember { Animatable(0f) }

    // Update isRefreshing based on state
    LaunchedEffect(state) {
        isRefreshing = state is RefreshState.Refreshing
        if (!isRefreshing) {
            // Reset pull offset when refresh completes
            indicatorOffsetAnim.animateTo(0f, animationSpec = tween(200))
        }
    }

    // Nested scroll connection to detect pull-to-refresh gesture
    val nestedScrollConnection = remember(listState, isRefreshing) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Check if we can scroll backward (up) - if not, we're at the top
                val atTop = !listState.canScrollBackward

                Timber.d("PullToRefresh onPreScroll: available=$available, atTop=$atTop, canScrollBackward=${listState.canScrollBackward}, pullOffset=$pullOffset, isRefreshing=$isRefreshing")

                // Only intercept if at top, not refreshing, and pulling down
                if (!atTop || isRefreshing) {
                    // If we're pulling but no longer at top, reset
                    if (pullOffset > 0 && !atTop) {
                        scope.launch {
                            pullOffset = 0f
                            indicatorOffsetAnim.animateTo(0f, animationSpec = tween(200))
                        }
                    }
                    return Offset.Zero
                }

                // If pulling down (positive y) when at top, intercept the gesture
                if (available.y > 0) {
                    val newOffset = (pullOffset + available.y).coerceAtMost(refreshThresholdPx * 1.5f)
                    pullOffset = newOffset
                    scope.launch {
                        indicatorOffsetAnim.snapTo(newOffset)
                    }
                    return Offset(0f, available.y)
                }

                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                // Check if we can scroll backward (up) - if not, we're at the top
                val atTop = !listState.canScrollBackward

                if (!atTop || isRefreshing) {
                    return Offset.Zero
                }

                // If there's remaining scroll after consumption and we're pulling down
                if (available.y > 0) {
                    val newOffset = (pullOffset + available.y).coerceAtMost(refreshThresholdPx * 1.5f)
                    pullOffset = newOffset
                    scope.launch {
                        indicatorOffsetAnim.snapTo(newOffset)
                    }
                    return Offset(0f, available.y)
                }

                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                // When user releases, check if we should trigger refresh
                if (pullOffset >= refreshThresholdPx && !isRefreshing) {
                    onRefresh()
                    pullOffset = 0f
                    indicatorOffsetAnim.snapTo(refreshThresholdPx / 2) // Keep indicator visible during refresh
                } else if (pullOffset > 0) {
                    // Animate back to 0 if threshold not reached
                    pullOffset = 0f
                    indicatorOffsetAnim.animateTo(0f, animationSpec = tween(200))
                }
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
    ) {
        // Main content
        content()

        // Pull-to-refresh indicator
        if (isRefreshing || indicatorOffsetAnim.value.absoluteValue > 0.1f) {
            val progress = min(1f, indicatorOffsetAnim.value / refreshThresholdPx)
            val alpha = if (isRefreshing) 1f else progress

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha),
                contentAlignment = Alignment.TopCenter,
            ) {
                if (isRefreshing) {
                    // Indeterminate progress during refresh
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(indicatorSize)
                            .offset(y = 8.dp),
                    )
                } else {
                    // Determinate progress based on pull distance
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(indicatorSize)
                            .offset(y = (indicatorOffsetAnim.value / 3).dp),
                        progress = progress,
                    )
                }
            }
        }
    }
}
