package au.com.shiftyjelly.pocketcasts.search

import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.ImprovedSearchResultItem
import au.com.shiftyjelly.pocketcasts.search.component.ImprovedSearchEpisodeResultRow
import au.com.shiftyjelly.pocketcasts.search.component.ImprovedSearchFolderResultRow
import au.com.shiftyjelly.pocketcasts.search.component.ImprovedSearchPodcastResultRow
import au.com.shiftyjelly.pocketcasts.search.component.NoResultsView
import au.com.shiftyjelly.pocketcasts.search.component.SearchFailedView
import au.com.shiftyjelly.pocketcasts.search.component.SearchResultFilters
import au.com.shiftyjelly.pocketcasts.views.helper.PlayButtonListener

@Composable
fun ImprovedSearchResultsPage(
    state: SearchUiState.ImprovedResults,
    loading: Boolean,
    bottomInset: Dp,
    onEpisodeClick: (ImprovedSearchResultItem.EpisodeItem) -> Unit,
    onPodcastClick: (ImprovedSearchResultItem.PodcastItem) -> Unit,
    onFolderClick: (Folder, List<Podcast>) -> Unit,
    onFollowPodcast: (ImprovedSearchResultItem.PodcastItem) -> Unit,
    onFilterSelect: (ResultsFilters) -> Unit,
    playButtonListener: PlayButtonListener,
    onScroll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        when (val operation = state.operation) {
            is SearchUiState.SearchOperation.Error -> SearchFailedView()
            is SearchUiState.SearchOperation.Success -> {
                ImprovedSearchResultsView(
                    state = operation,
                    bottomInset = bottomInset,
                    onEpisodeClick = onEpisodeClick,
                    onPodcastClick = onPodcastClick,
                    onFolderClick = onFolderClick,
                    onFollowPodcast = onFollowPodcast,
                    playButtonListener = playButtonListener,
                    onScroll = onScroll,
                    selectedFilterIndex = state.selectedFilterIndex,
                    filterOptions = state.filterOptions.toList(),
                    onFilterSelect = onFilterSelect,
                )
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
    state: SearchUiState.SearchOperation.Success<SearchResults.ImprovedResults>,
    bottomInset: Dp,
    onEpisodeClick: (ImprovedSearchResultItem.EpisodeItem) -> Unit,
    onPodcastClick: (ImprovedSearchResultItem.PodcastItem) -> Unit,
    onFolderClick: (Folder, List<Podcast>) -> Unit,
    onFollowPodcast: (ImprovedSearchResultItem.PodcastItem) -> Unit,
    playButtonListener: PlayButtonListener,
    onScroll: () -> Unit,
    filterOptions: List<ResultsFilters>,
    selectedFilterIndex: Int,
    onFilterSelect: (ResultsFilters) -> Unit,
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

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            bottom = bottomInset,
        ),
        modifier = modifier
            .nestedScroll(nestedScrollConnection),
    ) {
        stickyHeader {
            SearchResultFilters(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colors.background,
                    )
                    .padding(16.dp),
                items = filterOptions.map { stringResource(it.resId) },
                selectedIndex = selectedFilterIndex,
                onFilterSelect = { onFilterSelect(filterOptions[it]) },
            )
        }

        if (state.results.filteredResults.isEmpty()) {
            item {
                NoResultsView()
            }
        } else {
            val dividerModifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
            state.results.filteredResults.forEachIndexed { index, item ->
                when (item) {
                    is ImprovedSearchResultItem.FolderItem -> {
                        item(key = "folder-${item.uuid}", contentType = "folder") {
                            ImprovedSearchFolderResultRow(
                                folderItem = item,
                                onClick = { onFolderClick(item.folder, item.podcasts) },
                            )
                        }
                    }

                    is ImprovedSearchResultItem.PodcastItem -> {
                        item(key = "podcast-${item.uuid}", contentType = "podcast") {
                            ImprovedSearchPodcastResultRow(
                                podcastItem = item,
                                onClick = { onPodcastClick(item) },
                                onFollow = { onFollowPodcast(item) },
                            )
                        }
                    }

                    is ImprovedSearchResultItem.EpisodeItem -> {
                        item(key = "episode-${item.uuid}", contentType = "episode") {
                            ImprovedSearchEpisodeResultRow(
                                episode = item,
                                onClick = { onEpisodeClick(item) },
                                playButtonListener = playButtonListener,
                            )
                        }
                    }
                }

                if (index < state.results.filteredResults.lastIndex) {
                    item {
                        HorizontalDivider(dividerModifier)
                    }
                }
            }
        }
    }
}
