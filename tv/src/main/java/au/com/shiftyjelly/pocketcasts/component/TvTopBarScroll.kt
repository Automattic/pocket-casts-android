package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import au.com.shiftyjelly.pocketcasts.theme.TvTopBarHeight
import kotlin.math.roundToInt

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

val LocalTopBarScrollState = staticCompositionLocalOf { TvTopBarScrollState() }

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
    DisposableEffect(listState) {
        onDispose { topBar.set(0f) }
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
    DisposableEffect(gridState) {
        onDispose { topBar.set(0f) }
    }
}

/**
 * Insets content below the top bar so directional focus can still descend into it from the top
 * navigation, then translates it up in step with the bar as the active list scrolls, so the bar and
 * content slide out of view together. The inset is a layout offset rather than top padding so the
 * content keeps its full height and, once the bar has retracted, fills the screen instead of leaving
 * a bar-height gap of dead space at the bottom that clips the last rows.
 */
@Composable
internal fun Modifier.scrollAwayTopBar(): Modifier {
    val topBar = LocalTopBarScrollState.current
    return this
        .offset { IntOffset(x = 0, y = TvTopBarHeight.roundToPx()) }
        .graphicsLayer { translationY = -topBar.offsetPx }
}

/**
 * Collapses a header (e.g. the search field) out of view in step with the top bar, so it scrolls
 * away with the content instead of staying pinned once the bar has retracted, matching tvOS. The
 * header shrinks its own height, slides up and fades as the active list scrolls, then expands again
 * as the list returns to the top. Pass [enabled] = false to keep the header fixed.
 */
@Composable
internal fun Modifier.collapseWithTopBar(enabled: Boolean = true): Modifier {
    if (!enabled) {
        return this
    }
    val topBar = LocalTopBarScrollState.current
    val barHeightPx = with(LocalDensity.current) { TvTopBarHeight.toPx() }
    return this
        .clipToBounds()
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            val fraction = (topBar.offsetPx / barHeightPx).coerceIn(0f, 1f)
            val height = (placeable.height * (1f - fraction)).roundToInt()
            layout(placeable.width, height) {
                placeable.placeWithLayer(0, -(placeable.height - height)) { alpha = 1f - fraction }
            }
        }
}
