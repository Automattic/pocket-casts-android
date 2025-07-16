package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/* This wrapper class allows the pager to adjust its height based on the tallest page. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HorizontalPagerWrapper(
    pageCount: Int,
    initialPage: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showPageIndicator: Boolean = true,
    pageIndicatorColor: Color = Color.White,
    pageSize: PageSize = PageSize.Fixed(LocalConfiguration.current.screenWidthDp.dp - 1.dp), // With full page width, height is not adjusted properly
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable (Int, Int, FocusRequester) -> Unit = { _, _, _ -> },
) {
    val pagerState = rememberPagerState(initialPage = initialPage) { pageCount }

    LaunchedEffect(pagerState, onPageChange) {
        snapshotFlow { pagerState.currentPage }.collect { index ->
            onPageChange(index)
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val focusRequesters = remember(pageCount) { List(pageCount) { FocusRequester() } }
    val subContentFocusRequesters = remember(pageCount) { List(pageCount) { FocusRequester() } }

    var pagerHeight by remember { mutableIntStateOf(0) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        HorizontalPager(
            state = pagerState,
            pageSize = pageSize,
            contentPadding = contentPadding,
        ) { index ->
            var pageHeight by remember { mutableIntStateOf(0) }
            var isSubContentFocused by remember { mutableStateOf(false) }
            Box(
                Modifier
                    .semantics {
                        contentDescription = "Page ${index + 1} of $pageCount"
                    }
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
                    .onFocusChanged {
                        if (it.isFocused) {
                            isSubContentFocused = false
                        }
                    }
                    .focusRequester(focusRequesters[index])
                    .focusable()
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.DirectionRight -> {
                                    val next = (index + 1).coerceAtMost(pageCount - 1)
                                    focusRequesters[next].requestFocus()
                                    coroutineScope.launch {
                                        pagerState.scrollToPage(next)
                                    }
                                    isSubContentFocused = false
                                    true
                                }
                                Key.DirectionLeft -> {
                                    val prev = (index - 1).coerceAtLeast(0)
                                    focusRequesters[prev].requestFocus()
                                    coroutineScope.launch {
                                        pagerState.scrollToPage(prev)
                                    }
                                    isSubContentFocused = false
                                    true
                                }
                                Key.DirectionDown -> {
                                    if (!isSubContentFocused) {
                                        subContentFocusRequesters[index].requestFocus()
                                        isSubContentFocused = true
                                        true
                                    } else {
                                        false
                                    }
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    },
            ) {
                content(index, pagerHeight, subContentFocusRequesters[index])
            }
        }

        if (showPageIndicator) {
            PagerDotIndicator(
                state = pagerState,
                activeDotColor = pageIndicatorColor,
                modifier = Modifier.height(40.dp),
            )
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
