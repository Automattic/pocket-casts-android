package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.annotation.FloatRange
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlin.math.abs

@Composable
fun FadedLazyColumn(
    modifier: Modifier = Modifier,
    fadeConfig: FadeConfig = FadeConfig.Default,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (reverseLayout) Arrangement.Bottom else Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    val density = LocalDensity.current
    val edgeState = remember(fadeConfig, state, density) {
        FadedEdgeState(
            config = fadeConfig,
            listState = state,
            density = density,
        )
    }

    LazyColumn(
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        content = content,
        modifier = modifier
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                with(edgeState) { drawStartRect() }
                with(edgeState) { drawEndRect() }
            },
    )
}

@Composable
fun FadedLazyRow(
    modifier: Modifier = Modifier,
    fadeConfig: FadeConfig = FadeConfig.Default,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal = if (reverseLayout) Arrangement.End else Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    val density = LocalDensity.current
    val edgeState = remember(fadeConfig, state, density) {
        FadedEdgeState(
            config = fadeConfig,
            listState = state,
            density = density,
        )
    }

    LazyRow(
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        content = content,
        modifier = modifier
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                with(edgeState) { drawStartRect() }
                with(edgeState) { drawEndRect() }
            },
    )
}

data class FadeConfig(
    val threshold: Dp,
    val size: FadeSize,
    val showStartFade: Boolean,
    val showEndFade: Boolean,
) {
    companion object {
        val Default = FadeConfig(
            threshold = 32.dp,
            size = FadeSize.Relative(0.125f),
            showStartFade = true,
            showEndFade = true,
        )
    }
}

sealed interface FadeSize {
    @JvmInline
    value class Fixed(
        val value: Dp,
    ) : FadeSize {
        init {
            check(value > 0.dp) { "Fade size must be positive: $value" }
        }
    }

    @JvmInline
    value class Relative(
        @FloatRange(0.0, 1.0, fromInclusive = false) val value: Float,
    ) : FadeSize {
        init {
            check(value > 0.0f && value <= 1.0f) {
                "Fade size must be between 0.0 (exclusive) and 1.0 (inclusive): $value"
            }
        }
    }
}

private class FadedEdgeState(
    private val config: FadeConfig,
    private val listState: LazyListState,
    private val density: Density,
) {
    private val layoutInfo get() = listState.layoutInfo

    private val boxSize by derivedStateOf {
        val viewportSize = layoutInfo.viewportSize.toSize()
        when (layoutInfo.orientation) {
            Orientation.Vertical -> viewportSize.copy(
                height = when (val fadeSize = config.size) {
                    is FadeSize.Fixed -> density.run { fadeSize.value.toPx() }
                    is FadeSize.Relative -> viewportSize.height * fadeSize.value
                },
            )
            Orientation.Horizontal -> viewportSize.copy(
                width = when (val fadeSize = config.size) {
                    is FadeSize.Fixed -> density.run { fadeSize.value.toPx() }
                    is FadeSize.Relative -> viewportSize.width * fadeSize.value
                },
            )
        }
    }

    private val startAlpha by derivedStateOf {
        val firstItemInfo = layoutInfo.visibleItemsInfo.firstOrNull() ?: return@derivedStateOf 0f
        val lastItemInfo = layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf 0f

        val lastItemIndex = layoutInfo.totalItemsCount - 1

        val viewportMainAxisSize = layoutInfo.viewportSize.mainAxis
        val spacingSize = layoutInfo.mainAxisItemSpacing * lastItemIndex
        val visibleItemsSize = layoutInfo.visibleItemsInfo.sumOf { it.size } + spacingSize
        val clippingSize = visibleItemsSize - viewportMainAxisSize

        val isFirstVisible = firstItemInfo.index == 0
        val isLastClipped = lastItemInfo.index == lastItemIndex && clippingSize > 0

        if (isFirstVisible) {
            val fadeThreshold = minOf(
                density.run { config.threshold.toPx() },
                firstItemInfo.size.toFloat(),
                if (isLastClipped) clippingSize.toFloat() else Float.MAX_VALUE,
            )
            val unpaddedOffset = abs(firstItemInfo.offset) - layoutInfo.beforeContentPadding
            if (unpaddedOffset > 0) {
                unpaddedOffset / fadeThreshold
            } else {
                0f
            }
        } else {
            1f
        }
    }

    private val endAlpha by derivedStateOf {
        val firstItemInfo = layoutInfo.visibleItemsInfo.firstOrNull() ?: return@derivedStateOf 0f
        val lastItemInfo = layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf 0f

        val lastItemIndex = layoutInfo.totalItemsCount - 1

        val viewportMainAxisSize = layoutInfo.viewportSize.mainAxis
        val spacingSize = layoutInfo.mainAxisItemSpacing * lastItemIndex
        val visibleItemsSize = layoutInfo.visibleItemsInfo.sumOf { it.size } + spacingSize
        val clippingSize = visibleItemsSize - viewportMainAxisSize

        val isLastVisible = lastItemInfo.index == lastItemIndex
        val isFirstClipped = firstItemInfo.index == 0 && clippingSize > 0

        if (isLastVisible) {
            val fadeThreshold = minOf(
                density.run { config.threshold.toPx() },
                lastItemInfo.size.toFloat(),
                if (isFirstClipped) clippingSize.toFloat() else Float.MAX_VALUE,
            )
            val lastUnpaddedVisibleSize = viewportMainAxisSize - lastItemInfo.offset - layoutInfo.afterContentPadding
            val unpaddedOffsetLeft = lastItemInfo.size - lastUnpaddedVisibleSize
            if (unpaddedOffsetLeft > 0) {
                unpaddedOffsetLeft / fadeThreshold
            } else {
                0f
            }
        } else {
            1f
        }
    }

    fun ContentDrawScope.drawStartRect() {
        if (config.showStartFade) {
            drawRect(
                brush = gradientBrush(
                    0f to Color.Black.copy(alpha = startAlpha),
                    0.15f to Color.Black.copy(alpha = startAlpha),
                    1f to Color.Transparent,
                    start = 0f,
                    end = boxSize.mainAxis,
                ),
                size = boxSize,
                topLeft = Offset.Zero,
                blendMode = BlendMode.DstOut,
            )
        }
    }

    fun ContentDrawScope.drawEndRect() {
        if (config.showEndFade) {
            val viewportMainAxis = layoutInfo.viewportSize.mainAxis.toFloat()
            val endOffset = viewportMainAxis - boxSize.mainAxis
            drawRect(
                brush = gradientBrush(
                    0f to Color.Black.copy(alpha = endAlpha),
                    0.15f to Color.Black.copy(alpha = endAlpha),
                    1f to Color.Transparent,
                    start = viewportMainAxis,
                    end = endOffset,
                ),
                size = boxSize,
                topLeft = offset(mainAxis = endOffset),
                blendMode = BlendMode.DstOut,
            )
        }
    }

    private val Size.mainAxis get() = when (listState.layoutInfo.orientation) {
        Orientation.Vertical -> height
        Orientation.Horizontal -> width
    }

    private val IntSize.mainAxis get() = when (listState.layoutInfo.orientation) {
        Orientation.Vertical -> height
        Orientation.Horizontal -> width
    }

    private fun gradientBrush(
        vararg colorStops: Pair<Float, Color>,
        start: Float = 0f,
        end: Float = Float.POSITIVE_INFINITY,
    ) = when (listState.layoutInfo.orientation) {
        Orientation.Vertical -> Brush.verticalGradient(
            colorStops = colorStops,
            startY = start,
            endY = end,
        )
        Orientation.Horizontal -> Brush.horizontalGradient(
            colorStops = colorStops,
            startX = start,
            endX = end,
        )
    }

    private fun offset(
        mainAxis: Float,
    ) = when (listState.layoutInfo.orientation) {
        Orientation.Vertical -> Offset(x = 0f, y = mainAxis)
        Orientation.Horizontal -> Offset(x = mainAxis, y = 0f)
    }
}
