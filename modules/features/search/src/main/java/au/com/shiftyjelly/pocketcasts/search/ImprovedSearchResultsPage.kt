package au.com.shiftyjelly.pocketcasts.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeItem
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.search.component.ImprovedSearchEpisodeResultRow
import au.com.shiftyjelly.pocketcasts.search.component.ImprovedSearchPodcastResultRow
import au.com.shiftyjelly.pocketcasts.search.component.NoResultsView
import au.com.shiftyjelly.pocketcasts.search.component.SearchFailedView
import au.com.shiftyjelly.pocketcasts.search.component.SearchResultFilters

@Composable
fun ImprovedSearchResultsPage(
    state: SearchUiState.Results,
    loading: Boolean,
    bottomInset: Dp,
    onEpisodeClick: (EpisodeItem) -> Unit,
    onPodcastClick: (Podcast) -> Unit,
    onFolderClick: (Folder, List<Podcast>) -> Unit,
    onFollowPodcast: (Podcast) -> Unit,
    onPlayEpisode: (EpisodeItem) -> Unit,
    onScroll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
    ) {
        when (val operation = state.operation) {
            is SearchUiState.SearchOperation.Error -> SearchFailedView()
            is SearchUiState.SearchOperation.Success -> {
                if (operation.results.isEmpty) {
                    NoResultsView()
                } else {
                    ImprovedSearchResultsView(
                        state = operation,
                        bottomInset = bottomInset,
                        onEpisodeClick = onEpisodeClick,
                        onPodcastClick = onPodcastClick,
                        onFolderClick = onFolderClick,
                        onFollowPodcast = onFollowPodcast,
                        onPlayEpisode = onPlayEpisode,
                        onScroll = onScroll,
                    )
                }
            }

            else -> Unit
        }
        if (loading) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth(),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.theme.colors.secondaryIcon01,
                )
            }
        }
    }
}

@Composable
private fun ImprovedSearchResultsView(
    state: SearchUiState.SearchOperation.Success<SearchResults>,
    bottomInset: Dp,
    onEpisodeClick: (EpisodeItem) -> Unit,
    onPodcastClick: (Podcast) -> Unit,
    onFolderClick: (Folder, List<Podcast>) -> Unit,
    onFollowPodcast: (Podcast) -> Unit,
    onPlayEpisode: (EpisodeItem) -> Unit,
    onScroll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                onScroll()
                return super.onPostFling(consumed, available)
            }
        }
    }
    val listState = rememberLazyListState()
    var selectedPillIndex by remember { mutableIntStateOf(0) }
    val filters = listOf("Top Results", "Podcasts", "Episodes")

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottomInset
        ),
        modifier = modifier
            .nestedScroll(nestedScrollConnection),
    ) {

        item {
            SearchResultFilters(
                items = filters,
                selectedIndex = selectedPillIndex,
                onFilterSelected = { selectedPillIndex = filters.indexOf(it) }
            )
        }

        state.results.podcasts.forEachIndexed { index, item ->
            item {
                ImprovedSearchPodcastResultRow(
                    folderItem = item,
                    onClick = {
                        when (item) {
                            is FolderItem.Folder -> onFolderClick(item.folder, item.podcasts)
                            is FolderItem.Podcast -> onPodcastClick(item.podcast)
                        }
                    },
                    onFollow = {
                        if (item is FolderItem.Podcast) {
                            onFollowPodcast(item.podcast)
                        } else {
                            null
                        }
                    },
                )
            }

            if (index < state.results.podcasts.lastIndex) {
                item {
                    HorizontalDivider()
                }
            }
        }

        if (state.results.episodes.isNotEmpty()) {
            item {
                HorizontalDivider()
            }
        }

       state.results.episodes.forEachIndexed { index, item ->
           item {
               ImprovedSearchEpisodeResultRow(
                   episode = item,
                   onClick = { onEpisodeClick(item) },
                   onPlay = { onPlayEpisode(item) }
               )
           }
           if (index < state.results.episodes.lastIndex) {
               item {
                   HorizontalDivider()
               }
           }
        }
    }
}