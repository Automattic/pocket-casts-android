package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import au.com.shiftyjelly.pocketcasts.theme.TvTopBarHeight

/**
 * Holds the current scroll offset of the active tab's content so the top bar and screen titles can
 * scroll out of view with it, matching tvOS rather than staying pinned at the top.
 */
@Stable
class TvTopBarScrollState {
    var offsetPx by mutableFloatStateOf(0f)

    internal fun set(offsetPx: Float) {
        this.offsetPx = offsetPx
    }
}

val LocalTopBarScrollState = staticCompositionLocalOf<TvTopBarScrollState> {
    error("TvTopBarScrollState was not provided")
}

/**
 * Reports the scroll position of [listState] to [LocalTopBarScrollState]. The first item of the
 * list must be a spacer of exactly [TvTopBarHeight] so a fully scrolled-out spacer reads as a
 * fully hidden top bar.
 */
@Composable
internal fun TopBarScrollReporter(listState: LazyListState) {
    val topBar = LocalTopBarScrollState.current
    val barHeightPx = with(LocalDensity.current) { TvTopBarHeight.toPx() }
    LaunchedEffect(listState) {
        snapshotFlow {
            if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset else Int.MAX_VALUE
        }.collect { offset -> topBar.set(offset.toFloat().coerceAtMost(barHeightPx)) }
    }
}

@Composable
internal fun TopBarScrollReporter(gridState: LazyGridState) {
    val topBar = LocalTopBarScrollState.current
    val barHeightPx = with(LocalDensity.current) { TvTopBarHeight.toPx() }
    LaunchedEffect(gridState) {
        snapshotFlow {
            if (gridState.firstVisibleItemIndex == 0) gridState.firstVisibleItemScrollOffset else Int.MAX_VALUE
        }.collect { offset -> topBar.set(offset.toFloat().coerceAtMost(barHeightPx)) }
    }
}

/**
 * Lays content out below the top bar so directional focus can still descend into it from the top
 * navigation, then translates it up in step with the bar as the active list scrolls, so the bar and
 * content slide out of view together.
 */
@Composable
internal fun Modifier.scrollAwayTopBar(): Modifier {
    val topBar = LocalTopBarScrollState.current
    return this
        .padding(top = TvTopBarHeight)
        .graphicsLayer { translationY = -topBar.offsetPx }
}
