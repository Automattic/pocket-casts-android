package au.com.shiftyjelly.pocketcasts.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastItem
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.search.component.SearchFolderRow
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
) {
    val state = viewModel.searchNewResults.collectAsState(
        SearchState.Results(
            searchTerm = "",
            list = emptyList(),
            error = null,
            loading = false
        )
    )
    when (state.value) {
        is SearchState.NoResults -> NoResultsView()
        is SearchState.Results -> {
            val result = state.value as SearchState.Results
            if (result.error == null || !onlySearchRemote || result.loading) {
                SearchResultsView(
                    state = state.value as SearchState.Results,
                    onPodcastClick = onPodcastClick,
                    onFolderClick = onFolderClick,
                    onScroll = onScroll,
                )
            } else {
                SearchFailedView()
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
private fun NoResultsView(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.search),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon01),
            modifier = modifier
                .size(96.dp)
                .padding(top = 32.dp, bottom = 16.dp)
        )
        TextH20(
            text = stringResource(LR.string.search_no_podcasts_found),
            modifier = modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )
        TextP50(
            text = stringResource(LR.string.search_no_podcasts_found_summary),
            modifier = modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            color = MaterialTheme.theme.colors.primaryText02
        )
    }
}

@Composable
private fun SearchFailedView(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = IR.drawable.search_failed),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon01),
            modifier = modifier
                .size(96.dp)
                .padding(top = 32.dp, bottom = 16.dp)
        )
        TextH20(
            text = stringResource(LR.string.error_search_failed),
            modifier = modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )
        TextP50(
            text = stringResource(LR.string.search_no_podcasts_found_summary),
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
