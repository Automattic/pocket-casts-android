package au.com.shiftyjelly.pocketcasts.player.view.chapters

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.models.to.Chapter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChaptersPage(
    lazyListState: LazyListState,
    chapters: List<ChaptersViewModel.ChapterState>,
    showHeader: Boolean,
    totalChaptersCount: Int,
    onSelectionChange: (Boolean, Chapter) -> Unit,
    onChapterClick: (Chapter) -> Unit,
    onUrlClick: (Chapter) -> Unit,
    onSkipChaptersClick: (Boolean) -> Unit,
    isTogglingChapters: Boolean,
    showSubscriptionIcon: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = lazyListState,
        modifier = modifier
            .background(LocalChaptersTheme.current.background)
            .fillMaxSize()
            .padding(top = if (showHeader) 0.dp else 16.dp),
    ) {
        val selectedCount = chapters.count { it.chapter.selected }
        if (showHeader) {
            item {
                ChaptersHeader(
                    totalChaptersCount = totalChaptersCount,
                    hiddenChaptersCount = totalChaptersCount - selectedCount,
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
                selectedCount = selectedCount,
                onSelectionChange = onSelectionChange,
                onClick = { onChapterClick(state.chapter) },
                onUrlClick = { onUrlClick(state.chapter) },
                modifier = Modifier.animateItem(),
            )
            if (index < chapters.lastIndex) {
                Divider(
                    color = LocalChaptersTheme.current.divider,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
    }
}
