package au.com.shiftyjelly.pocketcasts.player.view.shelf

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfViewModel.Companion.moreActionsTitle
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfViewModel.Companion.shortcutTitle
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfItem
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfRowItem
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfTitle
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun MenuShelfItems(
    shelfViewModel: ShelfViewModel = hiltViewModel<ShelfViewModel, ShelfViewModel.Factory> { factory ->
        factory.create(episode.uuid)
    },
    shelfItems: List<ShelfRowItem>,
    episode: BaseEpisode,
    isEditable: Boolean,
    normalBackgroundColor: Color = Color.Transparent,
    selectedBackgroundColor: Color = Color.Black,
    onShelfItemMove: ((List<ShelfRowItem>, from: Int, to: Int) -> List<ShelfRowItem>?)? = null,
    onClick: ((ShelfItem, Boolean) -> Unit)? = null,
) {
    val uiState by shelfViewModel.uiState.collectAsStateWithLifecycle(null)
    Content(
        shelfItems = shelfItems,
        episode = episode,
        selectedBackgroundColor = selectedBackgroundColor,
        normalBackgroundColor = normalBackgroundColor,
        isEditable = isEditable,
        isTranscriptAvailable = uiState?.isTranscriptAvailable == true,
        onClick = onClick,
        onMove = { items, from, to ->
            onShelfItemMove?.invoke(items, from, to)
        },
    )
}

@Composable
private fun Content(
    shelfItems: List<ShelfRowItem>,
    episode: BaseEpisode,
    selectedBackgroundColor: Color,
    normalBackgroundColor: Color,
    isEditable: Boolean,
    isTranscriptAvailable: Boolean,
    onClick: ((ShelfItem, Boolean) -> Unit)? = null,
    onMove: (List<ShelfRowItem>, from: Int, to: Int) -> List<ShelfRowItem>?,
) {
    val lazyListState = rememberLazyListState()
    var items by rememberUpdatedState(shelfItems) as MutableState
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onMove(items, from.index, to.index).let { it?.let { items = it } }
    }

    LazyColumn(state = lazyListState) {
        items.forEachIndexed { index, listItem ->
            when (listItem) {
                is ShelfItem -> {
                    item(key = listItem.id) {
                        ReorderableItem(reorderableLazyListState, key = listItem.id) { isDragging ->
                            val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)
                            val color = if (isDragging) selectedBackgroundColor else normalBackgroundColor
                            val rowDraggableModifier = if (isEditable) Modifier.draggableHandle() else Modifier
                            Surface(elevation = elevation, color = color) {
                                ShelfItemRow(
                                    episode = episode,
                                    item = listItem,
                                    isEditable = isEditable,
                                    isTranscriptAvailable = isTranscriptAvailable,
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
    }
}

@Preview
@Composable
private fun MenuShelfItemsContentPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        Content(
            shelfItems = buildList {
                add(shortcutTitle)
                addAll(ShelfItem.entries.toList())
                add(5, moreActionsTitle)
            },
            episode = PodcastEpisode("", publishedDate = Date()),
            selectedBackgroundColor = Color.Transparent,
            normalBackgroundColor = Color.Transparent,
            isEditable = true,
            isTranscriptAvailable = true,
            onMove = { items, from, to -> items },
        )
    }
}
