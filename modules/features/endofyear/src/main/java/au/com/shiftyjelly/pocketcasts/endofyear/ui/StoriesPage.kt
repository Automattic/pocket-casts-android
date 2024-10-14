package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.endofyear.Story
import au.com.shiftyjelly.pocketcasts.endofyear.UiState
import kotlinx.coroutines.launch

@Composable
internal fun StoriesPage(
    state: UiState,
) {
    when (state) {
        is UiState.Synced -> Stories(state.stories)
        is UiState.Syncing -> LoadingIndicator()
        is UiState.Failure -> ErrorMessage()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Stories(
    stories: List<Story>,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val pagerState = rememberPagerState(pageCount = { stories.size })
        val coroutineScope = rememberCoroutineScope()
        val widthPx = LocalDensity.current.run { maxWidth.toPx() }

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (!pagerState.isScrollInProgress) {
                        coroutineScope.launch {
                            val nextPage = if (offset.x > widthPx / 2) {
                                pagerState.currentPage + 1
                            } else {
                                pagerState.currentPage - 1
                            }
                            pagerState.scrollToPage(nextPage)
                        }
                    }
                }
            },
        ) { index ->
            when (val story = stories[index]) {
                is Story.Cover -> StoryPlaceholder(story)
                is Story.NumberOfShows -> StoryPlaceholder(story)
                is Story.TopShow -> StoryPlaceholder(story)
                is Story.TopShows -> StoryPlaceholder(story)
                is Story.Ratings -> StoryPlaceholder(story)
                is Story.TotalTime -> StoryPlaceholder(story)
                is Story.LongestEpisode -> StoryPlaceholder(story)
                is Story.PlusInterstitial -> StoryPlaceholder(story)
                is Story.YearVsYear -> StoryPlaceholder(story)
                is Story.CompletionRate -> StoryPlaceholder(story)
                is Story.Ending -> StoryPlaceholder(story)
            }
        }
    }
}

@Composable
private fun StoryPlaceholder(story: Story) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(story.backgroundColor),
    ) {
        TextP50("$story", color = Color.Black)
    }
}

private val Story.backgroundColor get() = when (this) {
    is Story.Cover -> Color(0xFFEE661C)
    is Story.NumberOfShows -> Color(0xFFEFECAD)
    is Story.TopShow -> Color(0xFFEDB0F3)
    is Story.TopShows -> Color(0xFFE0EFAD)
    is Story.Ratings -> Color(0xFFEFECAD)
    is Story.TotalTime -> Color(0xFFEDB0F3)
    is Story.LongestEpisode -> Color(0xFFE0EFAD)
    is Story.PlusInterstitial -> Color(0xFFEFECAD)
    is Story.YearVsYear -> Color(0xFFEEB1F4)
    is Story.CompletionRate -> Color(0xFFE0EFAD)
    is Story.Ending -> Color(0xFFEE661C)
}

@Composable
private fun LoadingIndicator() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Story.Cover.backgroundColor)
            .padding(16.dp),
    ) {
        LinearProgressIndicator(color = Color.Black)
    }
}

@Composable
private fun ErrorMessage() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Story.Cover.backgroundColor)
            .padding(16.dp),
    ) {
        TextH10(text = "Whoops!")
    }
}
