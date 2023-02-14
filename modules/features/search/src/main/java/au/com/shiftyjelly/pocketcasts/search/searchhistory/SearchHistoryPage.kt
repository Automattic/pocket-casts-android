package au.com.shiftyjelly.pocketcasts.search.searchhistory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.folder.FolderImageSmall
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.to.SearchHistoryEntry
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.UUID
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val IconSize = 48.dp
private const val CLEAR_ALL_THRESHOLD = 3

@Composable
internal fun SearchHistoryPage(
    viewModel: SearchHistoryViewModel,
    onClick: (SearchHistoryEntry) -> Unit,
    onShowClearAllConfirmation: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    SearchHistoryView(
        state = state,
        onCloseClick = { viewModel.remove(it) },
        onClearAllClick = {
            if (state.entries.size > CLEAR_ALL_THRESHOLD) {
                onShowClearAllConfirmation()
            } else {
                viewModel.clearAll()
            }
        },
        onRowClick = onClick,
    )
    viewModel.start()
}

@Composable
fun SearchHistoryView(
    state: SearchHistoryViewModel.State,
    onCloseClick: (SearchHistoryEntry) -> Unit,
    onClearAllClick: () -> Unit,
    onRowClick: (SearchHistoryEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.background(color = MaterialTheme.theme.colors.primaryUi01)
    ) {
        if (state.entries.isNotEmpty()) {
            item {
                Row(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextH20(
                        text = stringResource(LR.string.search_history_recent_searches),
                        color = MaterialTheme.theme.colors.primaryText01,
                        modifier = modifier.weight(1f)

                    )
                    TextP60(
                        text = stringResource(LR.string.clear_all).uppercase(),
                        color = MaterialTheme.theme.colors.support03,
                        fontWeight = FontWeight.W700,
                        modifier = modifier
                            .clickable { onClearAllClick() }
                            .padding(12.dp)
                    )
                }
            }
        }
        state.entries.forEach { entry ->
            item {
                when (entry) {
                    is SearchHistoryEntry.Episode -> Unit // TODO

                    is SearchHistoryEntry.Folder -> SearchHistoryRow(
                        content = { SearchHistoryFolderView(entry) },
                        onCloseClick = { onCloseClick(entry) },
                        onRowClick = { onRowClick(entry) },
                    )

                    is SearchHistoryEntry.Podcast -> SearchHistoryRow(
                        content = { SearchHistoryPodcastView(entry) },
                        onCloseClick = { onCloseClick(entry) },
                        onRowClick = { onRowClick(entry) },
                    )

                    is SearchHistoryEntry.SearchTerm -> SearchHistoryRow(
                        content = { SearchHistoryTermView(entry) },
                        onCloseClick = { onCloseClick(entry) },
                        onRowClick = { onRowClick(entry) },
                    )
                }
            }
        }
    }
}

@Composable
fun SearchHistoryRow(
    onCloseClick: () -> Unit,
    onRowClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .clickable { onRowClick() }
                .fillMaxWidth()
        ) {
            Box(Modifier.weight(weight = 1f, fill = true)) {
                content.invoke()
            }
            CloseButton(onCloseClick)
        }
        HorizontalDivider(startIndent = 16.dp)
    }
}

@Composable
private fun CloseButton(
    onCloseClick: () -> Unit,
) {
    IconButton(onClick = onCloseClick) {
        Icon(
            imageVector = NavigationButton.Close.image,
            contentDescription = stringResource(NavigationButton.Close.contentDescription),
            tint = MaterialTheme.theme.colors.primaryIcon02
        )
    }
}

@Composable
fun SearchHistoryFolderView(
    entry: SearchHistoryEntry.Folder,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.theme.colors.getFolderColor(entry.color)
    Column {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.theme.colors.primaryUi01)
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp)

        ) {
            FolderImageSmall(
                color = color,
                podcastUuids = entry.podcastIds,
                folderImageSize = IconSize,
                podcastImageSize = 20.dp
            )
            Column(
                modifier = modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                TextH40(
                    text = entry.title,
                    maxLines = 2,
                    color = MaterialTheme.theme.colors.primaryText01,
                    modifier = modifier.padding(bottom = 2.dp)
                )
                val podcastCount = if (entry.podcastIds.size == 1) {
                    stringResource(LR.string.podcasts_singular)
                } else {
                    stringResource(
                        LR.string.podcasts_plural,
                        entry.podcastIds.size
                    )
                }
                TextH50(
                    text = stringResource(LR.string.search_history_row_type_folder_subtitle, podcastCount),
                    maxLines = 1,
                    color = MaterialTheme.theme.colors.primaryText02,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun SearchHistoryPodcastView(
    entry: SearchHistoryEntry.Podcast,
    modifier: Modifier = Modifier,
) {
    Column {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp)
        ) {
            PodcastImage(
                uuid = entry.uuid,
                modifier = modifier
                    .size(IconSize)
            )
            val subTitle = if (entry.author.isNotEmpty()) {
                stringResource(LR.string.search_history_row_type_podcast_subtitle, entry.author)
            } else {
                stringResource(LR.string.podcast)
            }
            Column(
                modifier = modifier
                    .padding(start = 8.dp)
                    .weight(1f)
            ) {
                TextH40(
                    text = entry.title,
                    maxLines = 2,
                    color = MaterialTheme.theme.colors.primaryText01
                )
                TextH50(
                    text = subTitle,
                    maxLines = 1,
                    color = MaterialTheme.theme.colors.primaryText02,
                    modifier = modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun SearchHistoryTermView(
    entry: SearchHistoryEntry.SearchTerm,
    modifier: Modifier = Modifier,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.theme.colors.primaryUi01)
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Box(
                modifier = modifier.size(IconSize),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = IR.drawable.ic_search),
                    contentDescription = null,
                    tint = MaterialTheme.theme.colors.primaryIcon02
                )
            }
            TextH40(
                text = entry.term,
                color = MaterialTheme.theme.colors.primaryText01,
                maxLines = 2,
                modifier = modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            )
        }
    }
}

@Preview
@Composable
fun SearchHistoryViewPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        SearchHistoryView(
            state = SearchHistoryViewModel.State(
                entries = listOf(
                    SearchHistoryEntry.Folder(
                        uuid = UUID.randomUUID().toString(),
                        title = "Folder",
                        color = 0,
                        podcastIds = emptyList()
                    ),
                    SearchHistoryEntry.Podcast(
                        uuid = UUID.randomUUID().toString(),
                        title = "Title",
                        author = "Author",
                    ),
                    SearchHistoryEntry.SearchTerm(
                        term = "Search Term"
                    ),
                )
            ),
            onCloseClick = {},
            onClearAllClick = {},
            onRowClick = {},
        )
    }
}
