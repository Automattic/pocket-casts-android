package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/* This wrapper class allows the pager to adjust its height based on the tallest page. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HorizontalPagerWrapper(
    pageCount: Int,
    initialPage: Int,
    onPageChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showPageIndicator: Boolean = true,
    pageIndicatorColor: Color = Color.White,
    pageSize: PageSize = PageSize.Fixed(LocalConfiguration.current.screenWidthDp.dp - 1.dp), // With full page width, height is not adjusted properly
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable (Int, Int) -> Unit = { _, _ -> },
) {
    val pagerState = rememberPagerState(initialPage = initialPage)

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { index ->
            onPageChanged(index)
        }
    }

    var pagerHeight by remember { mutableStateOf(0) }
    Column(modifier = modifier) {
        HorizontalPager(
            pageCount = pageCount,
            state = pagerState,
            pageSize = pageSize,
            contentPadding = contentPadding,
        ) { index ->
            var pageHeight by remember { mutableStateOf(0) }
            Box(
                Modifier
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        /* https://github.com/google/accompanist/issues/1050#issuecomment-1097483476 */
                        val placeable = measurable.measure(constraints)
                        pageHeight = placeable.height
                        /* Restrict page height to the pager height */
                        layout(constraints.maxWidth, pagerHeight) {
                            placeable.placeRelative(0, 0)
                        }
                    }
                    .onGloballyPositioned {
                        /* Update pager height to the tallest page */
                        if (pageHeight > pagerHeight) {
                            pagerHeight = pageHeight
                        }
                    }
            ) {
                content(index, pagerHeight)
            }
        }

        if (showPageIndicator) {
            PageIndicator(
                pageCount = pageCount,
                pagerState = pagerState,
                color = pageIndicatorColor,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PageIndicator(
    pageCount: Int,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
) {
    Row(
        Modifier
            .height(40.dp)
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(pageCount) { iteration ->
            val circleColor =
                if (pagerState.currentPage == iteration) color else color.copy(alpha = 0.5f)
            Box(
                modifier = modifier
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(circleColor)
                    .size(8.dp)
            )
        }
    }
}
