package au.com.shiftyjelly.pocketcasts.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import au.com.shiftyjelly.pocketcasts.compose.components.FadedLazyColumn
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeItem
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.search.component.ImprovedSearchEpisodeResultRow
import au.com.shiftyjelly.pocketcasts.search.component.ImprovedSearchPodcastResultRow
import au.com.shiftyjelly.pocketcasts.search.component.NoResultsView
import au.com.shiftyjelly.pocketcasts.search.component.SearchFailedView
import au.com.shiftyjelly.pocketcasts.search.component.SearchResultFilters
import au.com.shiftyjelly.pocketcasts.views.helper.PlayButtonListener

@Composable
fun ImprovedSearchResultsPage(
    state: SearchUiState.Results,
    loading: Boolean,
    bottomInset: Dp,
    onEpisodeClick: (EpisodeItem) -> Unit,
    onPodcastClick: (Podcast) -> Unit,
    onFolderClick: (Folder, List<Podcast>) -> Unit,
    onFollowPodcast: (Podcast) -> Unit,
    onFilterSelect: (ResultsFilters) -> Unit,
    playButtonListener: PlayButtonListener,
    onScroll: () -> Unit,
    modifier: Modifier = Modifier,
    fetchEpisode: (suspend (EpisodeItem) -> BaseEpisode?)? = null,
) {
    Column(
        modifier = modifier,
    ) {
        // we still need the server piece for this, so temporarily i'm filtering results locally here...
        val selectedFilter = state.filterOptions.toList()[state.selectedFilterIndex]
        val operation = (state.operation as? SearchUiState.SearchOperation.Success)?.copy(
            results = SearchResults(
                podcasts = if (selectedFilter != ResultsFilters.EPISODES) {
                    state.operation.results.podcasts
                } else {
                    emptyList()
                },
                episodes = if (selectedFilter != ResultsFilters.PODCASTS) {
                    state.operation.results.episodes
                } else {
                    emptyList()
                },
            ),
        ) ?: state.operation

        when (operation) {
            is SearchUiState.SearchOperation.Error -> SearchFailedView()
            is SearchUiState.SearchOperation.Success -> {
                if (operation.results.isEmpty) {
                    NoResultsView()
                } else {
                    SearchResultFilters(
                        modifier = Modifier.padding(16.dp),
                        items = state.filterOptions.map { stringResource(it.resId) },
                        selectedIndex = state.selectedFilterIndex,
                        onFilterSelect = { onFilterSelect(state.filterOptions.toList()[it]) },
                    )
                    ImprovedSearchResultsView(
                        state = operation,
                        bottomInset = bottomInset,
                        onEpisodeClick = onEpisodeClick,
                        onPodcastClick = onPodcastClick,
                        onFolderClick = onFolderClick,
                        onFollowPodcast = onFollowPodcast,
                        playButtonListener = playButtonListener,
                        onScroll = onScroll,
                        fetchEpisode = fetchEpisode,
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
    playButtonListener: PlayButtonListener,
    onScroll: () -> Unit,
    modifier: Modifier = Modifier,
    fetchEpisode: (suspend (EpisodeItem) -> BaseEpisode?)? = null,
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

    FadedLazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = bottomInset,
        ),
        modifier = modifier
            .nestedScroll(nestedScrollConnection),
    ) {
        val dividerModifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
        state.results.podcasts.forEachIndexed { index, item ->
            item(key = "podcast-${item.uuid}", contentType = "podcast") {
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
                            Unit
                        }
                    },
                )
            }

            if (index < state.results.podcasts.lastIndex) {
                item {
                    HorizontalDivider(dividerModifier)
                }
            }
        }

        if (state.results.episodes.isNotEmpty()) {
            item {
                HorizontalDivider(dividerModifier)
            }
        }

        state.results.episodes.forEachIndexed { index, item ->
            item(key = "episode-${item.uuid}", contentType = "episode") {
                ImprovedSearchEpisodeResultRow(
                    episode = item,
                    onClick = { onEpisodeClick(item) },
                    playButtonListener = playButtonListener,
                    fetchEpisode = fetchEpisode,
                )
            }
            if (index < state.results.episodes.lastIndex) {
                item {
                    HorizontalDivider(dividerModifier)
                }
            }
        }
    }
}
