package au.com.shiftyjelly.pocketcasts.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
    onFolderClick: (Folder, List<Podcast>) -> Unit,
    onPodcastClick: (Podcast) -> Unit,
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    Column {
        ThemedTopAppBar(
            title = stringResource(LR.string.search_results_all_podcasts),
            bottomShadow = true,
            onNavigationClick = { onBackClick() },
        )
        SearchPodcastResultsView(
            state = state as SearchState.Results,
            onFolderClick = onFolderClick,
            onPodcastClick = onPodcastClick,
            onSubscribeClick = { viewModel.onSubscribeToPodcast(it) },
        )
    }
}

@Composable
private fun SearchPodcastResultsView(
    state: SearchState.Results,
    onFolderClick: (Folder, List<Podcast>) -> Unit,
    onPodcastClick: (Podcast) -> Unit,
    onSubscribeClick: (Podcast) -> Unit
) {
    LazyColumn {
        items(
            items = state.podcasts,
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
                            .background(color = MaterialTheme.theme.colors.primaryUi01)
                    )
                }
            }
        }
    }
}
