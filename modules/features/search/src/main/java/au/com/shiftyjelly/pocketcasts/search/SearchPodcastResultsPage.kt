package au.com.shiftyjelly.pocketcasts.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastItem
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.search.component.SearchFolderRow
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun SearchPodcastResultsPage(
    viewModel: SearchViewModel,
    bottomInset: Dp,
    onFolderClick: (Folder, List<Podcast>) -> Unit,
    onPodcastClick: (Podcast) -> Unit,
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    Column(
        modifier = modifier,
    ) {
        ThemedTopAppBar(
            title = stringResource(LR.string.search_results_all_podcasts),
            onNavigationClick = { onBackPress() },
        )
        ((state as? SearchUiState.OldResults?)?.operation as? SearchUiState.SearchOperation.Success)?.let {
            SearchPodcastResultsView(
                items = it.results.podcasts,
                onFolderClick = onFolderClick,
                onPodcastClick = onPodcastClick,
                onSubscribeClick = { viewModel.onSubscribeToPodcast(it) },
                bottomInset = bottomInset,
            )
        }
    }
}

@Composable
private fun SearchPodcastResultsView(
    items: List<FolderItem>,
    onFolderClick: (Folder, List<Podcast>) -> Unit,
    onPodcastClick: (Podcast) -> Unit,
    onSubscribeClick: (Podcast) -> Unit,
    bottomInset: Dp,
) {
    LazyColumn(
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomInset + 8.dp),
    ) {
        items(
            items = items,
            key = { it.adapterId },
        ) { folderItem ->
            when (folderItem) {
                is FolderItem.Folder -> {
                    SearchFolderRow(
                        folder = folderItem.folder,
                        podcasts = folderItem.podcasts,
                        onClick = { onFolderClick(folderItem.folder, folderItem.podcasts) },
                    )
                }

                is FolderItem.Podcast -> {
                    PodcastItem(
                        podcast = folderItem.podcast,
                        subscribed = folderItem.podcast.isSubscribed,
                        showSubscribed = true,
                        showPlusIfUnsubscribed = true,
                        maxLines = 2,
                        onClick = { onPodcastClick(folderItem.podcast) },
                        onPlusClick = { onSubscribeClick(folderItem.podcast) },
                        modifier = Modifier
                            .background(color = MaterialTheme.theme.colors.primaryUi01),
                    )
                }
            }
        }
    }
}
