package au.com.shiftyjelly.pocketcasts.playlists.manual

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.FadedLazyColumn
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBanner
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentData
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.folder.FolderImageSmall
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistEpisodeSource
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistFolderSource
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistPodcastSource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AddEpisodeSourcesColumn(
    sources: List<ManualPlaylistEpisodeSource>?,
    noContentData: NoContentData,
    onClickSource: (ManualPlaylistEpisodeSource) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(sources) {
        listState.scrollToItem(0)
    }

    FadedLazyColumn(
        state = listState,
        modifier = modifier,
    ) {
        if (!sources.isNullOrEmpty()) {
            item(key = "header", contentType = "header") {
                TextH30(
                    text = stringResource(LR.string.your_podcasts),
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                )
            }
            items(
                items = sources,
                key = { item ->
                    when (item) {
                        is ManualPlaylistFolderSource -> "folder:${item.uuid}"
                        is ManualPlaylistPodcastSource -> "podcast:${item.uuid}"
                    }
                },
                contentType = { item ->
                    when (item) {
                        is ManualPlaylistFolderSource -> "folder"
                        is ManualPlaylistPodcastSource -> "podcast"
                    }
                },
            ) { item ->
                EpisodeSourceRow(
                    source = item,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { onClickSource(item) })
                        .padding(vertical = 4.dp, horizontal = 16.dp),
                )
            }
        }
        if (sources?.isEmpty() == true) {
            item(key = "no-content", contentType = "no-content") {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                ) {
                    NoContentBanner(
                        data = noContentData,
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeSourceRow(
    source: ManualPlaylistEpisodeSource,
    modifier: Modifier = Modifier,
) {
    when (source) {
        is ManualPlaylistFolderSource -> FolderSourceRow(
            source = source,
            modifier = modifier,
        )

        is ManualPlaylistPodcastSource -> PodcastSourceRow(
            source = source,
            modifier = modifier,
        )
    }
}

@Composable
private fun PodcastSourceRow(
    source: ManualPlaylistPodcastSource,
    modifier: Modifier = Modifier,
) {
    BaseSourceRow(
        title = source.title,
        description = source.author,
        modifier = modifier,
    ) {
        PodcastImage(
            uuid = source.uuid,
        )
    }
}

@Composable
private fun FolderSourceRow(
    source: ManualPlaylistFolderSource,
    modifier: Modifier = Modifier,
) {
    val podcastCount = source.podcastSources.size
    BaseSourceRow(
        title = source.title,
        description = pluralStringResource(LR.plurals.podcasts_count, podcastCount, podcastCount),
        modifier = modifier,
    ) {
        FolderImageSmall(
            color = MaterialTheme.theme.colors.getFolderColor(source.color),
            podcastUuids = source.podcastSources.take(4),
        )
    }
}

@Composable
private fun BaseSourceRow(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    imageContent: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        imageContent()

        Spacer(
            modifier = Modifier.width(14.dp),
        )

        Column {
            TextH40(
                text = title,
                maxLines = 1,
            )
            TextP50(
                text = description,
                maxLines = 1,
                color = MaterialTheme.theme.colors.primaryText02,
            )
        }
    }
}

@Preview
@Composable
private fun EpisodeSourcesEmptyPreview() {
    AppThemeWithBackground(ThemeType.LIGHT) {
        AddEpisodeSourcesColumn(
            sources = emptyList(),
            NoContentData(
                title = "No podcasts found",
                body = "We couldn’t find any podcast for that search. Try another keyword.",
                iconId = IR.drawable.ic_exclamation_circle,
            ),
            onClickSource = {},
            modifier = Modifier.fillMaxHeight(),
        )
    }
}

@Preview
@Composable
private fun EpisodeSourcesColumnPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppThemeWithBackground(themeType) {
        AddEpisodeSourcesColumn(
            sources = listOf(
                ManualPlaylistPodcastSource(
                    uuid = "id-1",
                    title = "Podcast Title 1",
                    author = "Podcast Author 1",
                ),
                ManualPlaylistPodcastSource(
                    uuid = "id-2",
                    title = "Podcast Title 2",
                    author = "Podcast Author 2",
                ),
                ManualPlaylistPodcastSource(
                    uuid = "id-3",
                    title = "Podcast Title 3",
                    author = "Podcast Author 3",
                ),
                ManualPlaylistFolderSource(
                    uuid = "id-1",
                    title = "Folder Title 1",
                    color = 0,
                    podcastSources = List(10) { index -> "id-${index + 100}" },
                ),
                ManualPlaylistFolderSource(
                    uuid = "id-2",
                    title = "Folder Title 2",
                    color = 1,
                    podcastSources = List(2) { index -> "id-${index + 200}" },
                ),
            ),
            noContentData = NoContentData(
                title = "",
                body = "",
                iconId = IR.drawable.ic_exclamation_circle,
            ),
            onClickSource = {},
            modifier = Modifier.fillMaxHeight(),
        )
    }
}
