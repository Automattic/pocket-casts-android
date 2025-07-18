package au.com.shiftyjelly.pocketcasts.player.view.shelf

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfViewModel.Companion.moreActionsTitle
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfViewModel.Companion.shortcutTitle
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfViewModel.UiState
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfItem
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfTitle
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun MenuShelfItems(
    state: UiState,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedBackgroundColor: Color = Color.Black,
    normalBackgroundColor: Color = Color.Transparent,
    includeNavigationBarsPadding: Boolean = true,
    onClick: ((ShelfItem, Boolean) -> Unit)? = null,
) {
    val lazyListState = rememberLazyListState()
    val items by rememberUpdatedState(state.shelfRowItems)
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onMove(from.index, to.index)
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier,
    ) {
        items.forEach { listItem ->
            when (listItem) {
                is ShelfItem -> {
                    item(key = listItem.id) {
                        ReorderableItem(reorderableLazyListState, key = listItem.id) { isDragging ->
                            val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)
                            val color = if (isDragging) selectedBackgroundColor else normalBackgroundColor
                            val rowDraggableModifier = if (state.isEditable) Modifier.longPressDraggableHandle() else Modifier
                            Surface(elevation = elevation, color = color) {
                                ShelfItemRow(
                                    episode = state.episode,
                                    item = listItem,
                                    isEditable = state.isEditable,
                                    isTranscriptAvailable = state.isTranscriptAvailable,
                                    onClick = onClick,
                                    modifier = rowDraggableModifier,
                                )
                            }
                        }
                    }
                }

                is ShelfTitle -> {
                    item(key = listItem.title) {
                        ShelfTitleRow(item = listItem)
                    }
                }
            }
        }
        item {
            Spacer(
                modifier = Modifier
                    // add the bottom padding in the list so the content is under the navigation bar but last item is above it
                    .let { if (includeNavigationBarsPadding) it.navigationBarsPadding() else it }
                    .height(8.dp),
            )
        }
    }
}

@Preview
@Composable
private fun MenuShelfItemsNonEditableContentPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        MenuShelfItems(
            state = UiState(
                shelfRowItems = buildList {
                    addAll(ShelfItem.entries.toList().take(6))
                },
                episode = PodcastEpisode("", publishedDate = Date()),
                isEditable = false,
                isTranscriptAvailable = false,
            ),
            selectedBackgroundColor = Color.Transparent,
            normalBackgroundColor = Color.Transparent,
            onMove = { _, _ -> },
        )
    }
}

@Preview
@Composable
private fun MenuShelfItemsEditableContentPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        MenuShelfItems(
            state = UiState(
                shelfRowItems = buildList {
                    add(shortcutTitle)
                    addAll(ShelfItem.entries.toList())
                    add(5, moreActionsTitle)
                },
                episode = PodcastEpisode("", publishedDate = Date()),
                isEditable = true,
                isTranscriptAvailable = false,
            ),
            selectedBackgroundColor = Color.Transparent,
            normalBackgroundColor = Color.Transparent,
            onMove = { _, _ -> },
        )
    }
}
