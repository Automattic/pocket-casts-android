package au.com.shiftyjelly.pocketcasts.playlists

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
import au.com.shiftyjelly.pocketcasts.component.ScrollToTopEffect
import au.com.shiftyjelly.pocketcasts.component.TopBarScrollReporter
import au.com.shiftyjelly.pocketcasts.component.TvDetailOverlay
import au.com.shiftyjelly.pocketcasts.component.TvEmptyState
import au.com.shiftyjelly.pocketcasts.component.TvPlaylistCard
import au.com.shiftyjelly.pocketcasts.component.TvPlaylistCardColors
import au.com.shiftyjelly.pocketcasts.component.tvFocusInactiveWhen
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistIcon
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.playlists.details.TvPlaylistDetailsScreen
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreview
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylistPreview
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.TvTopBarHeight
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvPlaylistsScreen(
    modifier: Modifier = Modifier,
    viewModel: TvPlaylistsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isDownloadModalVisible by rememberSaveable { mutableStateOf(false) }
    var openedPlaylist by rememberSaveable(stateSaver = OpenedPlaylistSaver) { mutableStateOf<OpenedPlaylist?>(null) }
    var restoreFocusTrigger by remember { mutableIntStateOf(0) }
    var filterListShownTracked by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        val state = uiState
        if (!filterListShownTracked && state is TvPlaylistsUiState.Loaded) {
            viewModel.trackPlaylistsListShown(state.playlists.size)
            filterListShownTracked = true
        }
    }

    val playlist = openedPlaylist
    Box(modifier = modifier.fillMaxSize()) {
        TvPlaylistsContent(
            uiState = uiState,
            getArtworkUuidsFlow = viewModel::getArtworkUuidsFlow,
            getEpisodeCountFlow = viewModel::getEpisodeCountFlow,
            refreshArtworkUuids = viewModel::refreshArtworkUuids,
            refreshEpisodeCount = viewModel::refreshEpisodeCount,
            findPodcastTint = viewModel::findPodcastTint,
            onCreatePlaylist = {
                viewModel.trackCreateButtonTapped()
                isDownloadModalVisible = true
            },
            onOpenPlaylist = { preview ->
                isDownloadModalVisible = false
                openedPlaylist = OpenedPlaylist(preview.uuid, preview.type)
            },
            modifier = Modifier
                .fillMaxSize()
                .tvFocusInactiveWhen(playlist != null),
            restoreFocusTrigger = restoreFocusTrigger,
        )

        if (isDownloadModalVisible) {
            TvDownloadAppModal(
                onDismissRequest = { isDownloadModalVisible = false },
            )
        }

        TvDetailOverlay(
            target = playlist,
            onBack = { openedPlaylist = null },
            onHide = { restoreFocusTrigger++ },
        ) { openPlaylist ->
            TvPlaylistDetailsScreen(
                playlistUuid = openPlaylist.uuid,
                playlistType = openPlaylist.type,
                onClose = { openedPlaylist = null },
            )
        }
    }
}

private data class OpenedPlaylist(val uuid: String, val type: Playlist.Type)

private val OpenedPlaylistSaver = listSaver<OpenedPlaylist?, String>(
    save = { playlist -> playlist?.let { listOf(it.uuid, it.type.key) }.orEmpty() },
    restore = { saved ->
        saved.takeIf { it.size == 2 }?.let { (uuid, typeKey) ->
            Playlist.Type.fromValue(typeKey)?.let { type -> OpenedPlaylist(uuid, type) }
        }
    },
)

@Composable
private fun TvPlaylistsContent(
    uiState: TvPlaylistsUiState,
    getArtworkUuidsFlow: (String) -> StateFlow<List<String>?>,
    getEpisodeCountFlow: (String) -> StateFlow<Int?>,
    refreshArtworkUuids: suspend (String) -> Unit,
    refreshEpisodeCount: suspend (String) -> Unit,
    findPodcastTint: suspend (String) -> Int?,
    onCreatePlaylist: () -> Unit,
    onOpenPlaylist: (PlaylistPreview) -> Unit,
    modifier: Modifier = Modifier,
    restoreFocusTrigger: Int = 0,
) {
    AnimatedContent(
        targetState = uiState,
        transitionSpec = { fadeIn(tween(durationMillis = 300)) togetherWith fadeOut(tween(durationMillis = 300)) },
        contentKey = { state ->
            when (state) {
                is TvPlaylistsUiState.Loading -> "loading"
                is TvPlaylistsUiState.Loaded -> if (state.playlists.isEmpty()) "empty" else "content"
            }
        },
        label = "TvPlaylistsContent",
        modifier = modifier,
    ) { state ->
        when (state) {
            is TvPlaylistsUiState.Loading -> LoadingView(
                color = MaterialTheme.tvColors.textPrimary,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = TvTopBarHeight),
            )

            is TvPlaylistsUiState.Loaded -> if (state.playlists.isEmpty()) {
                TvPlaylistsEmpty(
                    onCreatePlaylist = onCreatePlaylist,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = TvTopBarHeight),
                )
            } else {
                TvPlaylistsGrid(
                    playlists = state.playlists,
                    getArtworkUuidsFlow = getArtworkUuidsFlow,
                    getEpisodeCountFlow = getEpisodeCountFlow,
                    refreshArtworkUuids = refreshArtworkUuids,
                    refreshEpisodeCount = refreshEpisodeCount,
                    findPodcastTint = findPodcastTint,
                    onOpenPlaylist = onOpenPlaylist,
                    modifier = Modifier.fillMaxSize(),
                    restoreFocusTrigger = restoreFocusTrigger,
                )
            }
        }
    }
}

@Composable
private fun TvPlaylistsGrid(
    playlists: List<PlaylistPreview>,
    getArtworkUuidsFlow: (String) -> StateFlow<List<String>?>,
    getEpisodeCountFlow: (String) -> StateFlow<Int?>,
    refreshArtworkUuids: suspend (String) -> Unit,
    refreshEpisodeCount: suspend (String) -> Unit,
    findPodcastTint: suspend (String) -> Int?,
    onOpenPlaylist: (PlaylistPreview) -> Unit,
    modifier: Modifier = Modifier,
    restoreFocusTrigger: Int = 0,
) {
    Column(modifier = modifier.padding(horizontal = 42.dp)) {
        var lastFocusedIndex by rememberSaveable(playlists) { mutableIntStateOf(0) }
        val focusRequesters = remember(playlists) { List(playlists.size) { FocusRequester() } }
        val gridFocusRequester = remember { FocusRequester() }
        val gridState = rememberLazyGridState()
        ScrollToTopEffect { gridState.scrollToItem(0) }
        TopBarScrollReporter(gridState)

        var isInitialComposition by remember { mutableStateOf(true) }
        LaunchedEffect(restoreFocusTrigger) {
            // Only restore on an actual trigger change, not when a fresh grid mounts with a
            // non-zero trigger inherited from a hoisted state holder.
            if (isInitialComposition) {
                isInitialComposition = false
            } else {
                runCatching { gridFocusRequester.requestFocus() }
            }
        }

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            modifier = Modifier
                .focusRequester(gridFocusRequester)
                .focusGroup()
                .focusProperties {
                    onEnter = {
                        runCatching { focusRequesters.getOrNull(lastFocusedIndex)?.requestFocus() }
                    }
                },
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(TvTopBarHeight))
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(LR.string.playlists),
                    style = MaterialTheme.tvTypography.title2,
                    color = MaterialTheme.tvColors.textPrimary,
                    modifier = Modifier.padding(top = 40.dp, bottom = 0.dp),
                )
            }
            itemsIndexed(
                items = playlists,
                key = { _, playlist -> playlist.uuid },
            ) { index, playlist ->
                TvPlaylistGridItem(
                    playlist = playlist,
                    getArtworkUuidsFlow = getArtworkUuidsFlow,
                    getEpisodeCountFlow = getEpisodeCountFlow,
                    refreshArtworkUuids = refreshArtworkUuids,
                    refreshEpisodeCount = refreshEpisodeCount,
                    findPodcastTint = findPodcastTint,
                    onOpenPlaylist = onOpenPlaylist,
                    modifier = Modifier
                        .focusRequester(focusRequesters[index])
                        .onFocusChanged { focusState ->
                            if (focusState.hasFocus) {
                                lastFocusedIndex = index
                            }
                        },
                )
            }
        }
    }
}

@Composable
private fun TvPlaylistGridItem(
    playlist: PlaylistPreview,
    getArtworkUuidsFlow: (String) -> StateFlow<List<String>?>,
    getEpisodeCountFlow: (String) -> StateFlow<Int?>,
    refreshArtworkUuids: suspend (String) -> Unit,
    refreshEpisodeCount: suspend (String) -> Unit,
    findPodcastTint: suspend (String) -> Int?,
    onOpenPlaylist: (PlaylistPreview) -> Unit,
    modifier: Modifier = Modifier,
) {
    val artworkUuids by remember(playlist.uuid) { getArtworkUuidsFlow(playlist.uuid) }.collectAsState()
    val episodeCount by remember(playlist.uuid) { getEpisodeCountFlow(playlist.uuid) }.collectAsState()

    LaunchedEffect(playlist.uuid, refreshArtworkUuids) {
        refreshArtworkUuids(playlist.uuid)
    }
    LaunchedEffect(playlist.uuid, refreshEpisodeCount) {
        refreshEpisodeCount(playlist.uuid)
    }

    var podcastTint by remember(playlist.uuid) { mutableStateOf<Int?>(null) }
    val coverPodcastUuid = artworkUuids?.firstOrNull()
    LaunchedEffect(coverPodcastUuid, findPodcastTint) {
        podcastTint = coverPodcastUuid?.let { findPodcastTint(it) }
    }

    TvPlaylistCard(
        title = playlist.title,
        isSmartPlaylist = playlist.type == Playlist.Type.Smart,
        episodeCount = episodeCount,
        artworkUrls = artworkUuids.orEmpty().map(PodcastImage::getMediumArtworkUrl),
        cardColor = TvPlaylistCardColors.cardColor(podcastTint, seed = playlist.uuid),
        onClick = { onOpenPlaylist(playlist) },
        modifier = modifier,
    )
}

@Composable
private fun TvPlaylistsEmpty(
    onCreatePlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvEmptyState(
        title = stringResource(LR.string.tv_playlists_empty_title),
        subtitle = stringResource(LR.string.tv_playlists_empty_subtitle),
        actionLabel = stringResource(LR.string.tv_playlists_empty_action_title),
        onAction = onCreatePlaylist,
        modifier = modifier,
    )
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvPlaylistsGridPreview() {
    TvTheme {
        Box(modifier = Modifier.background(MaterialTheme.tvColors.backgroundSunken)) {
            TvPlaylistsContent(
                uiState = TvPlaylistsUiState.Loaded(
                    playlists = List(5) { index -> previewPlaylist(index) },
                ),
                getArtworkUuidsFlow = { MutableStateFlow(listOf("podcast-1", "podcast-2")) },
                getEpisodeCountFlow = { MutableStateFlow(42) },
                refreshArtworkUuids = {},
                refreshEpisodeCount = {},
                findPodcastTint = { null },
                onCreatePlaylist = {},
                onOpenPlaylist = {},
            )
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvPlaylistsEmptyPreview() {
    TvTheme {
        Box(modifier = Modifier.background(MaterialTheme.tvColors.backgroundSunken)) {
            TvPlaylistsContent(
                uiState = TvPlaylistsUiState.Loaded(playlists = emptyList()),
                getArtworkUuidsFlow = { MutableStateFlow(null) },
                getEpisodeCountFlow = { MutableStateFlow(null) },
                refreshArtworkUuids = {},
                refreshEpisodeCount = {},
                findPodcastTint = { null },
                onCreatePlaylist = {},
                onOpenPlaylist = {},
            )
        }
    }
}

private fun previewPlaylist(index: Int) = SmartPlaylistPreview(
    uuid = "playlist-$index",
    title = "Playlist $index",
    settings = Playlist.Settings.ForPreview,
    icon = PlaylistIcon(0),
    smartRules = SmartRules.Default,
)
