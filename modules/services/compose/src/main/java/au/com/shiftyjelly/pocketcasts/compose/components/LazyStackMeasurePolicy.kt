@file:OptIn(ExperimentalFoundationApi::class)

package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastForEach

internal typealias LazyStackMeasurePolicy = LazyLayoutMeasureScope.(Constraints) -> LazyStackMeasureResult

@Composable
internal fun rememberLazyStackMeasurePolicy(
    state: LazyStackState,
    contentPadding: PaddingValues,
    pageProviderLambda: () -> LazyStackPageProvider,
): LazyStackMeasurePolicy {
    return remember(state, contentPadding) {
        lazyStackMeasurePolicy(state, contentPadding, pageProviderLambda)
    }
}

internal class LazyStackMeasureResult(
    val containerSize: Size?,
    val pageCount: Int,
    val firstVisiblePageIndex: Int?,
    val firstVisiblePageKey: Any?,
    val pageSize: Size?,
    val directionsForPage: Set<SwipeDirection>?,
    val onSwipedOut: ((SwipeDirection) -> Unit)?,
    val onSwipedIn: ((SwipeDirection) -> Unit)?,
    measureResult: MeasureResult,
) : MeasureResult by measureResult

private fun lazyStackMeasurePolicy(
    state: LazyStackState,
    contentPadding: PaddingValues,
    pageProviderLambda: () -> LazyStackPageProvider,
): LazyStackMeasurePolicy = { containerConstraints ->
    val pageProvider = pageProviderLambda()
    state.updatePremeasureConstraints(containerConstraints)

    val startPadding = contentPadding.calculateStartPadding(layoutDirection).roundToPx()
    val endPadding = contentPadding.calculateEndPadding(layoutDirection).roundToPx()
    val topPadding = contentPadding.calculateTopPadding().roundToPx()
    val bottomPadding = contentPadding.calculateBottomPadding().roundToPx()
    val horizontalPadding = startPadding + endPadding
    val verticalPadding = topPadding + bottomPadding
    val contentConstraints = containerConstraints.offset(-horizontalPadding, -verticalPadding)

    measureLazyStack(
        currentPageIndex = Snapshot.withoutReadObservation {
            state.matchPageIndexWithKey(pageProvider, state.currentPageIndex)
        },
        prefetchCount = state.pagePrefetchCount,
        offset = state.offset.round(),
        rotation = state.rotation,
        scale = state.scale,
        pageCount = pageProvider.itemCount,
        containerConstraints = containerConstraints,
        constraints = contentConstraints,
        pageProvider = pageProvider,
    ).also(state::applyMeasureResult)
}

private fun LazyLayoutMeasureScope.measureLazyStack(
    currentPageIndex: Int,
    prefetchCount: Int,
    offset: IntOffset,
    rotation: Float,
    scale: Float,
    pageCount: Int,
    containerConstraints: Constraints,
    constraints: Constraints,
    pageProvider: LazyStackPageProvider,
): LazyStackMeasureResult {
    val visiblePageIndex = if (currentPageIndex > pageCount) {
        pageCount - 1
    } else {
        currentPageIndex
    }
    if (visiblePageIndex == pageCount || pageCount <= 0) {
        return LazyStackMeasureResult(
            containerSize = null,
            pageCount = pageCount,
            firstVisiblePageIndex = null,
            firstVisiblePageKey = null,
            pageSize = null,
            directionsForPage = null,
            onSwipedOut = null,
            onSwipedIn = null,
            measureResult = layout(constraints.minWidth, constraints.minHeight, placementBlock = {}),
        )
    }

    val start = visiblePageIndex
    val end = (visiblePageIndex + prefetchCount + 1).coerceAtMost(pageCount - 1)
    val visibleIndices = start..end
    val visiblePages = visibleIndices.mapIndexed { localIndex, itemIndex ->
        MeasuredLazyStackPage(
            pageIndex = itemIndex,
            key = pageProvider.getKey(itemIndex),
            placeables = measure(itemIndex, constraints),
            offset = if (localIndex == 0) offset else IntOffset.Zero,
            rotation = if (localIndex == 0) rotation else 0f,
            scale = if (localIndex == 0) 1f else scale,
            zIndex = -localIndex.toFloat(),
        )
    }
    val firstPlaceable = visiblePages.firstOrNull()?.placeables?.firstOrNull()
    val size = firstPlaceable?.let { IntSize(it.width, it.height).toSize() }
    val modifier = firstPlaceable?.parentData as? SwipeableModifier
    return LazyStackMeasureResult(
        containerSize = IntSize(containerConstraints.maxWidth, containerConstraints.maxHeight).toSize(),
        firstVisiblePageIndex = visiblePages.firstOrNull()?.pageIndex,
        firstVisiblePageKey = visiblePages.firstOrNull()?.key,
        pageSize = size,
        directionsForPage = modifier?.directions,
        onSwipedOut = modifier?.onSwipedOut,
        onSwipedIn = modifier?.onSwipedIn,
        pageCount = pageCount,
        measureResult = layout(constraints.maxWidth, constraints.maxHeight) {
            visiblePages.fastForEach(::place)
        },
    )
}

private class MeasuredLazyStackPage(
    val pageIndex: Int,
    val key: Any?,
    val placeables: List<Placeable>,
    val offset: IntOffset,
    val rotation: Float,
    val scale: Float,
    val zIndex: Float,
)

private fun Placeable.PlacementScope.place(page: MeasuredLazyStackPage) = with(page) {
    placeables.fastForEach { placeable ->
        placeable.placeRelativeWithLayer(offset, zIndex = zIndex) {
            rotationZ = rotation
            scaleX = scale
            scaleY = scale
        }
    }
}
