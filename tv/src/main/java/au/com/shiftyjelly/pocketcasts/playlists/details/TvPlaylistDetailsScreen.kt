package au.com.shiftyjelly.pocketcasts.playlists.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.component.TvArchivedFilterButton
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeActionContext
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeActionsModal
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeInfoModal
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeListItem
import au.com.shiftyjelly.pocketcasts.component.TvSortButton
import au.com.shiftyjelly.pocketcasts.component.rememberTvEpisodeListFocus
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.PlaylistArtwork
import au.com.shiftyjelly.pocketcasts.compose.components.displayLabel
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.podcasts.TvPodcastDetailsScreen
import au.com.shiftyjelly.pocketcasts.repositories.playlist.ManualPlaylist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.availableSortTypes
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.theme.TvDetailsArtworkSize
import au.com.shiftyjelly.pocketcasts.theme.TvTextStyles
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
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
    var openedPodcastUuid by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState, onClose) {
        if (uiState is TvPlaylistDetailsUiState.NotFound) {
            onClose()
        }
    }

    val podcastUuid = openedPodcastUuid
    if (podcastUuid != null) {
        BackHandler { openedPodcastUuid = null }
        TvPodcastDetailsScreen(
            podcastUuid = podcastUuid,
            onClose = { openedPodcastUuid = null },
            modifier = modifier,
        )
    } else {
        TvPlaylistDetailsContent(
            uiState = uiState,
            onChangeSortType = viewModel::changeSortType,
            onToggleArchiveFilter = viewModel::toggleArchiveFilter,
            onOpenPodcast = { openedPodcastUuid = it },
            modifier = modifier,
        )
    }
}

@Composable
private fun TvPlaylistDetailsContent(
    uiState: TvPlaylistDetailsUiState,
    onChangeSortType: (PlaylistEpisodeSortType) -> Unit,
    onToggleArchiveFilter: () -> Unit,
    onOpenPodcast: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is TvPlaylistDetailsUiState.Loading, TvPlaylistDetailsUiState.NotFound -> {
                LoadingView(color = TvColors.TextPrimary, modifier = Modifier.fillMaxSize())
            }

            is TvPlaylistDetailsUiState.Loaded -> {
                val playAllFocusRequester = remember { FocusRequester() }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(80.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 32.dp, top = 16.dp, end = 32.dp),
                ) {
                    PlaylistInfo(
                        playlist = uiState.playlist,
                        episodes = uiState.episodes,
                        playAllFocusRequester = playAllFocusRequester,
                        modifier = Modifier.width(InfoPaneWidth),
                    )
                    if (uiState.availableEpisodeCount == 0) {
                        NoEpisodes(
                            playlistType = uiState.playlist.type,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    } else {
                        SortableEpisodeList(
                            uiState = uiState,
                            onChangeSortType = onChangeSortType,
                            onToggleArchiveFilter = onToggleArchiveFilter,
                            onOpenPodcast = onOpenPodcast,
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
    onToggleArchiveFilter: () -> Unit,
    onOpenPodcast: (String) -> Unit,
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 12.dp),
        ) {
            TvArchivedFilterButton(
                isShowingArchived = uiState.isShowingArchivedOnDevice,
                onToggleArchiveFilter = onToggleArchiveFilter,
                leftFocusRequester = playAllFocusRequester.takeIf { uiState.episodes.isNotEmpty() },
            )
            TvSortButton(
                selected = sortType,
                options = uiState.playlist.availableSortTypes,
                label = { it.displayLabel() },
                onSelect = onChangeSortType,
            )
        }
        if (uiState.episodes.isNotEmpty()) {
            EpisodeList(
                episodes = uiState.episodes,
                onOpenPodcast = onOpenPodcast,
                playAllFocusRequester = playAllFocusRequester,
                listState = listState,
                modifier = Modifier.weight(1f),
            )
        } else {
            AllEpisodesArchived(
                episodeCount = uiState.availableEpisodeCount,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AllEpisodesArchived(
    episodeCount: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Text(
            text = pluralStringResource(LR.plurals.tv_playlist_all_archived, episodeCount, episodeCount),
            style = MaterialTheme.typography.bodyLarge,
            color = TvColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 400.dp),
        )
    }
}

@Composable
private fun EpisodeList(
    episodes: List<PodcastEpisode>,
    onOpenPodcast: (String) -> Unit,
    playAllFocusRequester: FocusRequester,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dateFormatter = remember(context) { RelativeDateFormatter(context) }
    val focus = rememberTvEpisodeListFocus(episodes, listState, requestInitialFocus = true)
    var actionsEpisode by remember { mutableStateOf<PodcastEpisode?>(null) }
    var detailsEpisode by remember { mutableStateOf<PodcastEpisode?>(null) }
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
    ) {
        itemsIndexed(
            items = episodes,
            key = { _, episode -> episode.uuid },
        ) { index, episode ->
            TvEpisodeListItem(
                episode = episode,
                dateFormatter = dateFormatter,
                onClick = {},
                onOpenActions = {
                    focus.watchForRemoval(episodes, index)
                    actionsEpisode = episode
                },
                episodeFocusRequester = focus.requesterFor(episode.uuid),
                leftFocusRequester = playAllFocusRequester,
            )
        }
    }
    actionsEpisode?.let { episode ->
        TvEpisodeActionsModal(
            episode = episode,
            actionContext = TvEpisodeActionContext.Playlist,
            onDismissRequest = { actionsEpisode = null },
            onShowEpisodeDetails = {
                detailsEpisode = episode
                actionsEpisode = null
            },
            onGoToPodcast = { onOpenPodcast(episode.podcastUuid) },
        )
    }
    detailsEpisode?.let { episode ->
        TvEpisodeInfoModal(
            episode = episode,
            onDismissRequest = { detailsEpisode = null },
        )
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
                style = TvTextStyles.Title3,
                color = TvColors.TextPrimary,
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
            artworkSize = TvDetailsArtworkSize,
            cornerSize = 8.dp,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = when (playlist.type) {
                    Playlist.Type.Manual -> stringResource(LR.string.playlist)
                    Playlist.Type.Smart -> stringResource(LR.string.smart_playlist)
                },
                style = TvTextStyles.Caption2,
                color = TvColors.TextSecondary,
            )
            Text(
                text = playlist.title,
                style = TvTextStyles.Title3,
                color = TvColors.TextPrimary,
            )
            Text(
                text = episodeSummaryText(episodes),
                style = TvTextStyles.Caption2,
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

private val InfoPaneWidth = 200.dp

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
                    isShowingArchivedOnDevice = false,
                ),
                onChangeSortType = {},
                onToggleArchiveFilter = {},
                onOpenPodcast = {},
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
                    isShowingArchivedOnDevice = false,
                ),
                onChangeSortType = {},
                onToggleArchiveFilter = {},
                onOpenPodcast = {},
            )
        }
    }
}
