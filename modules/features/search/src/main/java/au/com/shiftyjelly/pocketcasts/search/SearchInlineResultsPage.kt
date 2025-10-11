package au.com.shiftyjelly.pocketcasts.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeItem
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.search.SearchResultsFragment.Companion.ResultsType
import au.com.shiftyjelly.pocketcasts.search.component.NoResultsView
import au.com.shiftyjelly.pocketcasts.search.component.SearchEpisodeItem
import au.com.shiftyjelly.pocketcasts.search.component.SearchFailedView
import au.com.shiftyjelly.pocketcasts.search.component.SearchFolderItem
import au.com.shiftyjelly.pocketcasts.search.component.SearchPodcastItem
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.parseIsoDate
import java.util.Date
import java.util.UUID
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val MAX_ITEM_COUNT = 20

@Composable
fun SearchInlineResultsPage(
    state: SearchUiState.OldResults,
    loading: Boolean,
    bottomInset: Dp,
    onEpisodeClick: (EpisodeItem) -> Unit,
    onPodcastClick: (Podcast) -> Unit,
    onFolderClick: (Folder, List<Podcast>) -> Unit,
    onShowAllCLick: (ResultsType) -> Unit,
    onFollowPodcast: (Podcast) -> Unit,
    onScroll: () -> Unit,
    modifier: Modifier = Modifier,
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
                    SearchResultsView(
                        state = operation,
                        onEpisodeClick = onEpisodeClick,
                        onPodcastClick = onPodcastClick,
                        onFolderClick = onFolderClick,
                        onShowAllCLick = onShowAllCLick,
                        onFollowPodcast = onFollowPodcast,
                        onScroll = onScroll,
                        bottomInset = bottomInset,
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
private fun SearchResultsView(
    state: SearchUiState.SearchOperation.Success<SearchResults.SegregatedResults>,
    bottomInset: Dp,
    onEpisodeClick: (EpisodeItem) -> Unit,
    onPodcastClick: (Podcast) -> Unit,
    onFolderClick: (Folder, List<Podcast>) -> Unit,
    onShowAllCLick: (ResultsType) -> Unit,
    onFollowPodcast: (Podcast) -> Unit,
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
    val podcastsRowState = rememberLazyListState()
    val episodesRowState = rememberLazyListState()
    val initialPodcasts by remember(state.searchTerm) { mutableStateOf(state.results.podcasts) }
    // Reset podcast list scroll position when podcast list changes
    // We use podcast UUIDs as the key to avoid unnecessary resets when only subscription properties change
    LaunchedEffect(key1 = state.results.podcasts.map { it.uuid }) {
        if (state.results.podcasts.isNotEmpty() && state.results.podcasts != initialPodcasts) {
            podcastsRowState.scrollToItem(0)
        }
    }
    LaunchedEffect(key1 = state.results.episodes) {
        if (state.results.episodes.isNotEmpty()) {
            episodesRowState.scrollToItem(0)
        }
    }
    LazyColumn(
        state = episodesRowState,
        contentPadding = PaddingValues(bottom = bottomInset),
        modifier = modifier
            .nestedScroll(nestedScrollConnection),
    ) {
        if (state.results.podcasts.isNotEmpty()) {
            item {
                SearchResultsHeaderView(
                    title = stringResource(LR.string.podcasts),
                    onShowAllCLick = { onShowAllCLick(ResultsType.PODCASTS) },
                )
            }
        }
        item {
            LazyRow(
                state = podcastsRowState,
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                items(
                    items = state.results.podcasts.take(minOf(MAX_ITEM_COUNT, state.results.podcasts.size)),
                    key = { it.uuid },
                ) { folderItem ->
                    when (folderItem) {
                        is FolderItem.Folder -> {
                            SearchFolderItem(
                                folder = folderItem.folder,
                                podcasts = folderItem.podcasts,
                                onClick = { onFolderClick(folderItem.folder, folderItem.podcasts) },
                            )
                        }

                        is FolderItem.Podcast -> {
                            SearchPodcastItem(
                                podcast = folderItem.podcast,
                                onClick = { onPodcastClick(folderItem.podcast) },
                                onSubscribeClick = if (!folderItem.podcast.isSubscribed) {
                                    { onFollowPodcast(folderItem.podcast) }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
            }
        }
        if (state.results.podcasts.isNotEmpty() && state.results.episodes.isNotEmpty()) {
            item {
                HorizontalDivider(
                    startIndent = 16.dp,
                    modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                )
            }
        }
        if (state.results.episodes.isNotEmpty()) {
            item {
                SearchResultsHeaderView(
                    title = stringResource(LR.string.episodes),
                    onShowAllCLick = { onShowAllCLick(ResultsType.EPISODES) },
                )
            }
        }
        items(
            items = state.results.episodes.take(minOf(MAX_ITEM_COUNT, state.results.episodes.size)),
            key = { it.uuid },
        ) {
            SearchEpisodeItem(
                episode = it,
                onClick = onEpisodeClick,
            )
        }
    }
}

@Composable
private fun SearchResultsHeaderView(
    title: String,
    onShowAllCLick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextH20(
            text = title,
            color = MaterialTheme.theme.colors.primaryText01,
            modifier = Modifier.weight(1f),
        )
        TextP60(
            text = stringResource(LR.string.search_show_all).uppercase(),
            color = MaterialTheme.theme.colors.support03,
            fontWeight = FontWeight.W700,
            modifier = Modifier
                .clickable { onShowAllCLick() }
                .padding(12.dp),
        )
    }
}

@Preview
@Composable
private fun SearchResultsViewPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        SearchResultsView(
            state = SearchUiState.SearchOperation.Success(
                searchTerm = "",
                results = SearchResults.SegregatedResults(
                    podcasts = listOf(
                        FolderItem.Folder(
                            folder = Folder(
                                uuid = UUID.randomUUID().toString(),
                                name = "Folder",
                                color = 0,
                                addedDate = Date(),
                                podcastsSortType = PodcastsSortType.NAME_A_TO_Z,
                                deleted = false,
                                syncModified = 0L,
                                sortPosition = 0,
                            ),
                            podcasts = listOf(Podcast(uuid = UUID.randomUUID().toString())),
                        ),
                        FolderItem.Podcast(
                            podcast = Podcast(
                                uuid = UUID.randomUUID().toString(),
                                title = "Podcast",
                                author = "Author",
                            ),
                        ),
                    ),
                    episodes = listOf(
                        EpisodeItem(
                            uuid = "6946de68-7fa7-48b0-9066-a7d6e1be2c07",
                            title = "Society's Challenges",
                            duration = 4004.0,
                            publishedAt = "2022-10-28T03:00:00Z".parseIsoDate() ?: Date(),
                            podcastUuid = "e7a6f7d0-02f2-0133-1c51-059c869cc4eb",
                            podcastTitle = "Material",
                        ),
                    ),
                ),
            ),
            onEpisodeClick = {},
            onPodcastClick = {},
            onFolderClick = { _, _ -> },
            onShowAllCLick = {},
            onFollowPodcast = {},
            onScroll = {},
            bottomInset = 0.dp,
        )
    }
}
