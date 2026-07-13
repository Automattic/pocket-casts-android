package au.com.shiftyjelly.pocketcasts.player.view.chapters

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChaptersPage(
    lazyListState: LazyListState,
    chapters: List<ChaptersViewModel.ChapterState>,
    showHeader: Boolean,
    hasGeneratedChapters: Boolean,
    totalChaptersCount: Int,
    onSelectionChange: (Boolean, Chapter) -> Unit,
    onChapterClick: (Chapter) -> Unit,
    onUrlClick: (Chapter) -> Unit,
    onSkipChaptersClick: (Boolean) -> Unit,
    onHideGeneratedChaptersClick: () -> Unit,
    isTogglingChapters: Boolean,
    showSubscriptionIcon: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = lazyListState,
        contentPadding = if (Build.VERSION.SDK_INT > 29) {
            WindowInsets.navigationBars.asPaddingValues()
        } else {
            PaddingValues(bottom = 56.dp)
        },
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
        if (hasGeneratedChapters) {
            item {
                Column {
                    TextP50(
                        text = stringResource(LR.string.chapters_generated_disclaimer),
                        color = LocalChaptersTheme.current.headerTitle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                    TextH50(
                        text = stringResource(LR.string.chapters_hide_generated),
                        color = LocalChaptersTheme.current.headerToggle,
                        modifier = Modifier
                            .clickable(onClick = onHideGeneratedChaptersClick)
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
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
