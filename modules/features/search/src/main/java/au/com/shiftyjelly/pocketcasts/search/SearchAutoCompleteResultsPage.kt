package au.com.shiftyjelly.pocketcasts.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.to.SearchAutoCompleteItem
import au.com.shiftyjelly.pocketcasts.search.component.ImprovedSearchEpisodeResultRow
import au.com.shiftyjelly.pocketcasts.search.component.ImprovedSearchFolderResultRow
import au.com.shiftyjelly.pocketcasts.search.component.ImprovedSearchPodcastResultRow
import au.com.shiftyjelly.pocketcasts.search.component.ImprovedSearchTermSuggestionRow
import au.com.shiftyjelly.pocketcasts.search.component.NoSuggestionsView
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.buttons.PlayButton
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun SearchAutoCompleteResultsPage(
    searchTerm: String,
    isLoading: Boolean,
    results: List<SearchAutoCompleteItem>,
    onTermClick: (SearchAutoCompleteItem.Term) -> Unit,
    onPodcastClick: (SearchAutoCompleteItem.Podcast) -> Unit,
    onPodcastFollow: (SearchAutoCompleteItem.Podcast) -> Unit,
    onEpisodeClick: (SearchAutoCompleteItem.Episode) -> Unit,
    onFolderClick: (SearchAutoCompleteItem.Folder) -> Unit,
    playButtonListener: PlayButton.OnClickListener,
    bottomInset: Dp,
    onScroll: () -> Unit,
    onReportSuggestionsRender: () -> Unit,
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

    CallOnce {
        onReportSuggestionsRender()
    }

    Box(
        modifier = modifier,
    ) {
        AnimatedVisibility(
            visible = isLoading,
            modifier = Modifier
                .padding(vertical = 32.dp)
                .align(Alignment.Center),
        ) {
            CircularProgressIndicator()
        }

        if (!isLoading && results.isEmpty()) {
            NoSuggestionsView()
        } else {
            LazyColumn(
                modifier = Modifier.nestedScroll(nestedScrollConnection),
                contentPadding = PaddingValues(bottom = bottomInset),
            ) {
                results.forEachIndexed { index, item ->
                    item(contentType = "content-${item.javaClass}") {
                        when (item) {
                            is SearchAutoCompleteItem.Term -> ImprovedSearchTermSuggestionRow(
                                searchTerm = searchTerm,
                                item = item,
                                onClick = { onTermClick(item) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                            )

                            is SearchAutoCompleteItem.Podcast -> ImprovedSearchPodcastResultRow(
                                item = item,
                                onClick = { onPodcastClick(item) },
                                onFollow = { onPodcastFollow(item) },
                                modifier = Modifier.fillMaxWidth(),
                            )

                            is SearchAutoCompleteItem.Episode -> ImprovedSearchEpisodeResultRow(
                                item = item,
                                onClick = { onEpisodeClick(item) },
                                playButtonListener = playButtonListener,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            is SearchAutoCompleteItem.Folder -> ImprovedSearchFolderResultRow(
                                folder = item,
                                onClick = { onFolderClick(item) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (results.indices.last != index) {
                        item(contentType = "divider") {
                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.theme.colors.secondaryText02,
                            )
                        }
                    }
                }

                if (results.isNotEmpty()) {
                    item {
                        TextP40(
                            modifier = Modifier
                                .semantics { role = Role.Button }
                                .clickable(
                                    onClick = {
                                        onTermClick(SearchAutoCompleteItem.Term(searchTerm))
                                    },
                                )
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                            text = stringResource(LR.string.search_suggestions_view_all, searchTerm),
                            color = MaterialTheme.theme.colors.primaryInteractive01,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewSearchAutoCompleteResultsPage(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        SearchAutoCompleteResultsPage(
            isLoading = false,
            searchTerm = "matching",
            results = listOf(
                SearchAutoCompleteItem.Term("matching text"),
                SearchAutoCompleteItem.Term("matching text longer"),
                SearchAutoCompleteItem.Term("text only matching later"),
                SearchAutoCompleteItem.Term("this doesn't match but why is it returned then?"),
                SearchAutoCompleteItem.Folder(uuid = "", title = "folder matching", podcasts = listOf(SearchAutoCompleteItem.Podcast(uuid = "", title = "Podcast", author = "", isSubscribed = true)), color = 0x00ffff),
                SearchAutoCompleteItem.Podcast(uuid = "", title = "Matching podcast subscribed", author = "Author2", isSubscribed = true),
                SearchAutoCompleteItem.Podcast(uuid = "", title = "Matching podcast", author = "Author", isSubscribed = false),
            ),
            onTermClick = {},
            onEpisodeClick = {},
            onPodcastClick = {},
            onPodcastFollow = {},
            onFolderClick = {},
            onScroll = {},
            onReportSuggestionsRender = {},
            playButtonListener = object : PlayButton.OnClickListener {
                override var source: SourceView = SourceView.SEARCH_RESULTS

                override fun onPlayClicked(episodeUuid: String) = Unit

                override fun onPauseClicked() = Unit

                override fun onPlayNext(episodeUuid: String) = Unit

                override fun onPlayLast(episodeUuid: String) = Unit

                override fun onDownload(episodeUuid: String) = Unit

                override fun onStopDownloading(episodeUuid: String) = Unit

                override fun onPlayedClicked(episodeUuid: String) = Unit
            },
            bottomInset = 0.dp,
        )
    }
}

@Preview
@Composable
private fun PreviewEmptySearchAutoCompleteResultsPage(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        SearchAutoCompleteResultsPage(
            isLoading = false,
            searchTerm = "matching",
            results = emptyList(),
            onTermClick = {},
            onEpisodeClick = {},
            onPodcastClick = {},
            onPodcastFollow = {},
            onFolderClick = {},
            onScroll = {},
            onReportSuggestionsRender = {},
            playButtonListener = object : PlayButton.OnClickListener {
                override var source: SourceView = SourceView.SEARCH_RESULTS

                override fun onPlayClicked(episodeUuid: String) = Unit

                override fun onPauseClicked() = Unit

                override fun onPlayNext(episodeUuid: String) = Unit

                override fun onPlayLast(episodeUuid: String) = Unit

                override fun onDownload(episodeUuid: String) = Unit

                override fun onStopDownloading(episodeUuid: String) = Unit

                override fun onPlayedClicked(episodeUuid: String) = Unit
            },
            bottomInset = 0.dp,
        )
    }
}
