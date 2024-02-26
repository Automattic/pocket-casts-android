package au.com.shiftyjelly.pocketcasts.player.view.chapters

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChaptersPage(
    lazyListState: LazyListState,
    chapters: List<ChaptersViewModel.ChapterState>,
    totalChaptersCount: Int,
    onSelectionChange: (Boolean, Chapter) -> Unit,
    onChapterClick: (Chapter, Boolean) -> Unit,
    onUrlClick: (String) -> Unit,
    onSkipChaptersClick: (Boolean) -> Unit,
    isTogglingChapters: Boolean,
    showSubscriptionIcon: Boolean,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = lazyListState,
        modifier = modifier
            .background(backgroundColor)
            .fillMaxSize()
            .padding(top = if (FeatureFlag.isEnabled(Feature.DESELECT_CHAPTERS)) 0.dp else 16.dp),
    ) {
        if (FeatureFlag.isEnabled(Feature.DESELECT_CHAPTERS)) {
            item {
                ChaptersHeader(
                    totalChaptersCount = totalChaptersCount,
                    hiddenChaptersCount = totalChaptersCount - chapters.filter { it.chapter.selected }.size,
                    onSkipChaptersClick = onSkipChaptersClick,
                    isTogglingChapters = isTogglingChapters,
                    showSubscriptionIcon = showSubscriptionIcon,
                )
            }
        }
        itemsIndexed(chapters, key = { _, state -> state.chapter.index }) { index, state ->
            ChapterRow(
                state = state,
                isTogglingChapters = isTogglingChapters,
                onSelectionChange = onSelectionChange,
                onClick = { onChapterClick(state.chapter, state is ChaptersViewModel.ChapterState.Playing) },
                onUrlClick = { onUrlClick(state.chapter.url.toString()) },
                modifier = Modifier.animateItemPlacement(),
            )
            if (index < chapters.lastIndex) {
                Divider(
                    color = MaterialTheme.theme.colors.playerContrast06,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
    }
}
