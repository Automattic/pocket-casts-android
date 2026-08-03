package au.com.shiftyjelly.pocketcasts.playlists.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.component.TvDropdownMenu
import au.com.shiftyjelly.pocketcasts.component.TvDropdownMenuItem
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeRow
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.PlaylistArtwork
import au.com.shiftyjelly.pocketcasts.compose.components.displayLabel
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.repositories.playlist.ManualPlaylist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.availableSortTypes
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.theme.TvTextStyles
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import kotlinx.coroutines.flow.first
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvPlaylistDetailsScreen(
    playlistUuid: String,
    playlistType: Playlist.Type,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TvPlaylistDetailsViewModel = hiltViewModel<TvPlaylistDetailsViewModel, TvPlaylistDetailsViewModel.Factory>(
        key = playlistUuid,
        creationCallback = { factory -> factory.create(playlistUuid, playlistType) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState, onClose) {
        if (uiState is TvPlaylistDetailsUiState.NotFound) {
            onClose()
        }
    }

    TvPlaylistDetailsContent(
        uiState = uiState,
        onChangeSortType = viewModel::changeSortType,
        modifier = modifier,
    )
}

@Composable
private fun TvPlaylistDetailsContent(
    uiState: TvPlaylistDetailsUiState,
    onChangeSortType: (PlaylistEpisodeSortType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is TvPlaylistDetailsUiState.Loading, TvPlaylistDetailsUiState.NotFound -> {
                LoadingView(color = Color.White, modifier = Modifier.fillMaxSize())
            }

            is TvPlaylistDetailsUiState.Loaded -> {
                val playAllFocusRequester = remember { FocusRequester() }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(80.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 32.dp, top = 16.dp, end = 56.dp),
                ) {
                    PlaylistInfo(
                        playlist = uiState.playlist,
                        episodes = uiState.episodes,
                        playAllFocusRequester = playAllFocusRequester,
                        modifier = Modifier.width(ArtworkSize),
                    )
                    if (uiState.episodes.isEmpty()) {
                        NoEpisodes(
                            playlistType = uiState.playlist.type,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    } else {
                        SortableEpisodeList(
                            uiState = uiState,
                            onChangeSortType = onChangeSortType,
                            playAllFocusRequester = playAllFocusRequester,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SortableEpisodeList(
    uiState: TvPlaylistDetailsUiState.Loaded,
    onChangeSortType: (PlaylistEpisodeSortType) -> Unit,
    playAllFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val sortType = uiState.playlist.settings.sortType
    var lastSortType by remember { mutableStateOf(sortType) }
    LaunchedEffect(sortType) {
        if (sortType != lastSortType) {
            lastSortType = sortType
            listState.scrollToItem(0)
        }
    }
    Column(modifier = modifier) {
        SortDropdownButton(
            selectedSortType = sortType,
            availableSortTypes = uiState.playlist.availableSortTypes,
            onSelectSortType = onChangeSortType,
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 12.dp),
        )
        EpisodeList(
            episodes = uiState.episodes,
            playAllFocusRequester = playAllFocusRequester,
            listState = listState,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SortDropdownButton(
    selectedSortType: PlaylistEpisodeSortType,
    availableSortTypes: List<PlaylistEpisodeSortType>,
    onSelectSortType: (PlaylistEpisodeSortType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { isExpanded = true },
            colors = IconButtonDefaults.colors(
                containerColor = TvColors.BgActive20,
                contentColor = Color.White,
                focusedContainerColor = Color.White,
                focusedContentColor = TvColors.Dark,
            ),
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_sort),
                contentDescription = stringResource(LR.string.sort_by),
                modifier = Modifier.size(20.dp),
            )
        }
        if (isExpanded) {
            TvDropdownMenu(
                title = stringResource(LR.string.sort_by),
                onDismissRequest = { isExpanded = false },
            ) {
                availableSortTypes.forEach { sortType ->
                    TvDropdownMenuItem(
                        label = sortType.displayLabel(),
                        isSelected = sortType == selectedSortType,
                        onClick = {
                            isExpanded = false
                            onSelectSortType(sortType)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeList(
    episodes: List<PodcastEpisode>,
    playAllFocusRequester: FocusRequester,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dateFormatter = remember(context) { RelativeDateFormatter(context) }
    val firstEpisodeFocusRequester = remember { FocusRequester() }
    val initialFocusIndex = remember {
        Snapshot.withoutReadObservation { listState.firstVisibleItemIndex }
            .coerceIn(0, episodes.lastIndex)
    }
    LaunchedEffect(Unit) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.any { it.index == initialFocusIndex } }.first { it }
        runCatching { firstEpisodeFocusRequester.requestFocus() }
            .onFailure { Timber.e(it, "Failed to focus the first visible playlist episode") }
    }
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
    ) {
        itemsIndexed(
            items = episodes,
            key = { _, episode -> episode.uuid },
        ) { index, episode ->
            TvEpisodeRow(
                episode = episode,
                onClick = {},
                dateFormatter = dateFormatter,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusProperties { left = playAllFocusRequester }
                    .then(if (index == initialFocusIndex) Modifier.focusRequester(firstEpisodeFocusRequester) else Modifier),
            )
        }
    }
}

@Composable
private fun NoEpisodes(
    playlistType: Playlist.Type,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(LR.string.tv_playlist_empty_title),
                style = TvTextStyles.ScreenTitle,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Text(
                text = when (playlistType) {
                    Playlist.Type.Manual -> stringResource(LR.string.tv_playlist_empty_subtitle_manual)
                    Playlist.Type.Smart -> stringResource(LR.string.tv_playlist_empty_subtitle)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = TvColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 400.dp),
            )
        }
    }
}

@Composable
private fun PlaylistInfo(
    playlist: Playlist,
    episodes: List<PodcastEpisode>,
    playAllFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(28.dp),
        modifier = modifier,
    ) {
        PlaylistArtwork(
            podcastUuids = playlist.metadata.artworkUuids,
            artworkSize = ArtworkSize,
            cornerSize = 8.dp,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = when (playlist.type) {
                    Playlist.Type.Manual -> stringResource(LR.string.playlist)
                    Playlist.Type.Smart -> stringResource(LR.string.smart_playlist)
                },
                style = TvTextStyles.PlaylistCardCaption,
                color = TvColors.TextSecondary,
            )
            Text(
                text = playlist.title,
                style = TvTextStyles.ScreenTitle,
                color = Color.White,
            )
            Text(
                text = episodeSummaryText(episodes),
                style = TvTextStyles.PlaylistCardCaption,
                color = TvColors.TextSecondary,
            )
        }
        if (episodes.isNotEmpty()) {
            Button(
                onClick = {},
                colors = TvButtonDefaults.filledButtonColors(),
                modifier = Modifier.focusRequester(playAllFocusRequester),
            ) {
                Text(stringResource(LR.string.tv_playlist_play_all))
            }
        }
    }
}

@Composable
private fun episodeSummaryText(episodes: List<PodcastEpisode>): String {
    val countText = pluralStringResource(LR.plurals.episode_count, episodes.size, episodes.size)
    return if (episodes.isEmpty()) {
        countText
    } else {
        val totalDurationMs = episodes.sumOf { episode -> episode.durationMs.toLong() }
        "$countText · ${TimeHelper.getTimeDurationShortString(totalDurationMs, LocalContext.current)}"
    }
}

private val ArtworkSize = 200.dp

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvPlaylistDetailsPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            TvPlaylistDetailsContent(
                uiState = TvPlaylistDetailsUiState.Loaded(
                    playlist = ManualPlaylist(
                        uuid = "playlist-uuid",
                        title = "New Releases",
                        episodes = emptyList(),
                        settings = Playlist.Settings.ForPreview,
                        metadata = Playlist.Metadata.ForPreview,
                    ),
                    episodes = emptyList(),
                ),
                onChangeSortType = {},
            )
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvPlaylistDetailsLoadedPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            val episodes = List(5) { index ->
                PodcastEpisode(
                    uuid = "episode-$index",
                    title = "Episode $index",
                    duration = 1800.0,
                    publishedDate = Date(0),
                )
            }
            TvPlaylistDetailsContent(
                uiState = TvPlaylistDetailsUiState.Loaded(
                    playlist = ManualPlaylist(
                        uuid = "playlist-uuid",
                        title = "New Releases",
                        episodes = episodes.map(PlaylistEpisode::Available),
                        settings = Playlist.Settings.ForPreview,
                        metadata = Playlist.Metadata.ForPreview,
                    ),
                    episodes = episodes,
                ),
                onChangeSortType = {},
            )
        }
    }
}
