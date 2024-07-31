package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sign

private val dotWidth = 8.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagerDotIndicator(
    state: PagerState,
    modifier: Modifier = Modifier,
    activeDotColor: Color = Color.White,
    inactiveDotColor: Color = activeDotColor.copy(alpha = 0.3f),
) {
    if (state.pageCount <= 0) {
        return
    }
    val dotWidthPx = LocalDensity.current.run { dotWidth.roundToPx() }
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dotWidth),
        ) {
            repeat(state.pageCount) {
                Box(
                    modifier = Modifier
                        .size(dotWidth)
                        .background(inactiveDotColor, CircleShape),
                )
            }
        }
        Box(
            modifier = Modifier
                .offset {
                    val currentPageIndex = state.currentPage
                    val currentPageOffset = state.currentPageOffsetFraction
                    val nextPageIndex = currentPageIndex + currentPageOffset.sign.toInt()
                    val scrollPosition = (nextPageIndex - currentPageIndex) * currentPageOffset.absoluteValue + currentPageIndex
                    val normalizedScrollPosition = scrollPosition.coerceIn(0f, (state.pageCount - 1).toFloat())
                    IntOffset(
                        x = (2 * dotWidthPx * normalizedScrollPosition).roundToInt(),
                        y = 0,
                    )
                }
                .size(dotWidth)
                .background(activeDotColor, CircleShape),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
private fun PagerDotIndicatorPreview() = Column(
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.background(Color.Black).padding(16.dp),
) {
    PagerDotIndicator(rememberPagerState(pageCount = { 3 }))
    PagerDotIndicator(rememberPagerState(initialPage = 1, initialPageOffsetFraction = -0.5f, pageCount = { 3 }))
    PagerDotIndicator(rememberPagerState(initialPage = 1, initialPageOffsetFraction = -0.25f, pageCount = { 3 }))
    PagerDotIndicator(rememberPagerState(initialPage = 1, pageCount = { 3 }))
    PagerDotIndicator(rememberPagerState(initialPage = 1, initialPageOffsetFraction = 0.25f, pageCount = { 3 }))
    PagerDotIndicator(rememberPagerState(initialPage = 1, initialPageOffsetFraction = 0.5f, pageCount = { 3 }))
    PagerDotIndicator(rememberPagerState(pageCount = { 0 }))
}
