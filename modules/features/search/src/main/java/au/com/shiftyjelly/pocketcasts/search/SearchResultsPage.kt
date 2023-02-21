package au.com.shiftyjelly.pocketcasts.search

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.asFlow
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastItem
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.search.component.SearchEpisodeItem
import au.com.shiftyjelly.pocketcasts.search.component.SearchFolderItem
import au.com.shiftyjelly.pocketcasts.search.component.SearchFolderRow
import au.com.shiftyjelly.pocketcasts.search.component.SearchPodcastItem
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import java.util.UUID
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun SearchResultsPage(
    viewModel: SearchViewModel,
    onPodcastClick: (Podcast) -> Unit,
    onFolderClick: (Folder, List<Podcast>) -> Unit,
    onScroll: () -> Unit,
    onlySearchRemote: Boolean,
    modifier: Modifier = Modifier,
) {
    val state = viewModel.searchResults.collectAsState(
        SearchState.Results(
            searchTerm = "",
            list = emptyList(),
            episodeItems = emptyList(),
            error = null,
            loading = false
        )
    )
    val loading = viewModel.loading.asFlow().collectAsState(false)
    Column {
        when (state.value) {
            is SearchState.NoResults -> NoResultsView()
            is SearchState.Results -> {
                val result = state.value as SearchState.Results
                if (result.error == null || !onlySearchRemote || result.loading) {
                    if (BuildConfig.SEARCH_IMPROVEMENTS_ENABLED) {
                        if (result.list.isNotEmpty()) {
                            SearchResultsView(
                                state = state.value as SearchState.Results,
                                onPodcastClick = onPodcastClick,
                                onFolderClick = onFolderClick,
                                onScroll = onScroll,
                            )
                        }
                    } else {
                        OldSearchResultsView(
                            state = state.value as SearchState.Results,
                            onPodcastClick = onPodcastClick,
                            onFolderClick = onFolderClick,
                            onScroll = onScroll,
                        )
                    }
                } else {
                    SearchFailedView()
                }
            }
        }
        if (loading.value) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
            ) {
                CircularProgressIndicator(
                    modifier = modifier.size(24.dp),
                    color = MaterialTheme.theme.colors.secondaryIcon01,
                )
            }
        }
    }
}

@Composable
private fun SearchResultsView(
    state: SearchState.Results,
    onPodcastClick: (Podcast) -> Unit,
    onFolderClick: (Folder, List<Podcast>) -> Unit,
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
    LazyColumn(
        modifier = modifier
            .nestedScroll(nestedScrollConnection)
    ) {
        item { SearchResultsHeaderView(title = stringResource(LR.string.podcasts)) }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 8.dp),) {
                items(
                    items = state.list,
                    key = { it.adapterId }
                ) { folderItem ->
                    when (folderItem) {
                        is FolderItem.Folder -> {
                            SearchFolderItem(
                                folder = folderItem.folder,
                                podcasts = folderItem.podcasts,
                                onClick = { onFolderClick(folderItem.folder, folderItem.podcasts) }
                            )
                        }

                        is FolderItem.Podcast -> {
                            SearchPodcastItem(
                                podcast = folderItem.podcast,
                                onClick = { onPodcastClick(folderItem.podcast) },
                            )
                        }
                    }
                }
            }
        }
        item { SearchResultsHeaderView(title = stringResource(LR.string.episodes)) }
        state.episodeItems.forEach {
            item {
                SearchEpisodeItem(
                    episode = it,
                    onClick = {},
                )
            }
        }
    }
}

@Composable
private fun SearchResultsHeaderView(
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, end = 4.dp,),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextH20(
            text = title,
            color = MaterialTheme.theme.colors.primaryText01,
            modifier = modifier.weight(1f)
        )
        TextP60(
            text = stringResource(LR.string.search_show_all).uppercase(),
            color = MaterialTheme.theme.colors.support03,
            fontWeight = FontWeight.W700,
            modifier = modifier
                .clickable { /* TODO */ }
                .padding(12.dp)
        )
    }
}

@Composable
private fun OldSearchResultsView(
    state: SearchState.Results,
    onPodcastClick: (Podcast) -> Unit,
    onFolderClick: (Folder, List<Podcast>) -> Unit,
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
    LazyColumn(
        modifier = modifier
            .background(color = MaterialTheme.theme.colors.primaryUi01)
            .nestedScroll(nestedScrollConnection)
    ) {
        items(
            items = state.list,
            key = { it.adapterId }
        ) { folderItem ->
            when (folderItem) {
                is FolderItem.Folder -> {
                    SearchFolderRow(
                        folderItem.folder,
                        folderItem.podcasts,
                        onClick = { onFolderClick(folderItem.folder, folderItem.podcasts) }
                    )
                }

                is FolderItem.Podcast -> {
                    PodcastItem(
                        podcast = folderItem.podcast,
                        subscribed = folderItem.podcast.isSubscribed,
                        showSubscribed = true,
                        onClick = { onPodcastClick(folderItem.podcast) },
                        modifier = Modifier
                            .background(color = MaterialTheme.theme.colors.primaryUi01)
                    )
                }
            }
        }
    }
}

@Composable
private fun NoResultsView() {
    MessageView(
        imageResId = R.drawable.search,
        titleResId = LR.string.search_no_podcasts_found,
        summaryResId = LR.string.search_no_podcasts_found_summary,
    )
}

@Composable
private fun SearchFailedView() {
    MessageView(
        imageResId = IR.drawable.search_failed,
        titleResId = LR.string.error_search_failed,
        summaryResId = LR.string.error_check_your_internet_connection,
    )
}

@Composable
private fun MessageView(
    @DrawableRes imageResId: Int,
    @StringRes titleResId: Int,
    @StringRes summaryResId: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon01),
            modifier = modifier
                .size(96.dp)
                .padding(top = 32.dp, bottom = 16.dp)
        )
        TextH20(
            text = stringResource(titleResId),
            modifier = modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )
        TextP50(
            text = stringResource(summaryResId),
            modifier = modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            color = MaterialTheme.theme.colors.primaryText02
        )
    }
}

@Preview
@Composable
fun SearchResultsViewPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        SearchResultsView(
            state = SearchState.Results(
                list = listOf(
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
                        podcasts = listOf(Podcast(uuid = UUID.randomUUID().toString()))
                    ),
                    FolderItem.Podcast(
                        podcast = Podcast(uuid = UUID.randomUUID().toString(), title = "Podcast", author = "Author")
                    )
                ),
                episodeItems = listOf(dummyEpisodeItem),
                error = null,
                loading = false,
                searchTerm = ""
            ),
            onPodcastClick = {},
            onFolderClick = { _, _ -> },
            onScroll = {},
        )
    }
}

@Preview
@Composable
fun OldSearchResultsViewPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        OldSearchResultsView(
            state = SearchState.Results(
                list = listOf(
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
                        podcasts = listOf(Podcast(uuid = UUID.randomUUID().toString()))
                    ),
                    FolderItem.Podcast(
                        podcast = Podcast(uuid = UUID.randomUUID().toString(), title = "Podcast", author = "Author")
                    )
                ),
                episodeItems = emptyList(),
                error = null,
                loading = false,
                searchTerm = ""
            ),
            onPodcastClick = {},
            onFolderClick = { _, _ -> },
            onScroll = {},
        )
    }
}

@Preview
@Composable
fun NoResultsViewPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        NoResultsView()
    }
}

@Preview
@Composable
fun SearchFailedViewPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        SearchFailedView()
    }
}
