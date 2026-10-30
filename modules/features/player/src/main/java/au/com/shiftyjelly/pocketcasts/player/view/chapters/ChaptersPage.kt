package au.com.shiftyjelly.pocketcasts.player.view.chapters

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
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
    hasGeneratedChapters: Boolean,
    totalChaptersCount: Int,
    onSelectionChange: (Boolean, Chapter) -> Unit,
    onChapterClick: (Chapter) -> Unit,
    onUrlClick: (Chapter) -> Unit,
    onSkipChaptersClick: (Boolean) -> Unit,
    onTurnOffGeneratedClick: () -> Unit,
    emptyState: ChaptersViewModel.EmptyState?,
    onTurnOnGeneratedClick: () -> Unit,
    onGenerateClick: () -> Unit,
    isTogglingChapters: Boolean,
    showSubscriptionIcon: Boolean,
    modifier: Modifier = Modifier,
    resolvingChapterIndex: Int? = null,
) {
    Crossfade(
        targetState = emptyState,
        label = "chaptersContent",
    ) { state ->
        when (state) {
            ChaptersViewModel.EmptyState.GeneratedChaptersOff -> ChaptersOffState(
                onTurnOnClick = onTurnOnGeneratedClick,
                modifier = modifier,
            )

            ChaptersViewModel.EmptyState.NoChaptersYet -> ChaptersNoChaptersYet(
                onGenerateClick = onGenerateClick,
                modifier = modifier,
            )

            ChaptersViewModel.EmptyState.GeneratingChapters -> ChaptersGenerating(
                onTurnOffGeneratedClick = onTurnOffGeneratedClick,
                modifier = modifier,
            )

            ChaptersViewModel.EmptyState.NoGeneratedChapters -> ChaptersNoGeneratedChapters(
                modifier = modifier,
            )

            null -> ChaptersList(
                lazyListState = lazyListState,
                chapters = chapters,
                showHeader = showHeader,
                hasGeneratedChapters = hasGeneratedChapters,
                totalChaptersCount = totalChaptersCount,
                onSelectionChange = onSelectionChange,
                onChapterClick = onChapterClick,
                onUrlClick = onUrlClick,
                onSkipChaptersClick = onSkipChaptersClick,
                onTurnOffGeneratedClick = onTurnOffGeneratedClick,
                isTogglingChapters = isTogglingChapters,
                showSubscriptionIcon = showSubscriptionIcon,
                resolvingChapterIndex = resolvingChapterIndex,
                modifier = modifier,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChaptersList(
    lazyListState: LazyListState,
    chapters: List<ChaptersViewModel.ChapterState>,
    showHeader: Boolean,
    hasGeneratedChapters: Boolean,
    totalChaptersCount: Int,
    onSelectionChange: (Boolean, Chapter) -> Unit,
    onChapterClick: (Chapter) -> Unit,
    onUrlClick: (Chapter) -> Unit,
    onSkipChaptersClick: (Boolean) -> Unit,
    onTurnOffGeneratedClick: () -> Unit,
    isTogglingChapters: Boolean,
    showSubscriptionIcon: Boolean,
    resolvingChapterIndex: Int?,
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
                    isGenerated = hasGeneratedChapters,
                    onTurnOffGeneratedClick = onTurnOffGeneratedClick,
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
                isResolving = state.chapter.index == resolvingChapterIndex,
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
