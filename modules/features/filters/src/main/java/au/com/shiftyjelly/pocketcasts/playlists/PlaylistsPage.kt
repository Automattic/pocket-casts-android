package au.com.shiftyjelly.pocketcasts.playlists

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.Banner
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBanner
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.reorderable.rememberReorderableLazyListDataSource
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistIcon
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.playlists.PlaylistsViewModel.PlaylistsState
import au.com.shiftyjelly.pocketcasts.playlists.PlaylistsViewModel.UiState
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistPreviewRow
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistTooltip
import au.com.shiftyjelly.pocketcasts.playlists.component.SwipeToDeleteAnchor
import au.com.shiftyjelly.pocketcasts.repositories.playlist.ManualPlaylistPreview
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreview
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylistPreview
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun PlaylistsPage(
    uiState: UiState,
    getArtworkUuidsFlow: (String) -> StateFlow<List<String>?>,
    getEpisodeCountFlow: (String) -> StateFlow<Int?>,
    refreshArtworkUuids: suspend (String) -> Unit,
    refreshEpisodeCount: suspend (String) -> Unit,
    onCreatePlaylist: () -> Unit,
    onDeletePlaylist: (PlaylistPreview, settleRow: () -> Unit) -> Unit,
    onOpenPlaylist: (PlaylistPreview) -> Unit,
    onReorderPlaylists: (List<String>) -> Unit,
    onShowPlaylists: (List<PlaylistPreview>) -> Unit,
    onFreeAccountBannerCtaClick: () -> Unit,
    onFreeAccountBannerDismiss: () -> Unit,
    onShowPremadePlaylistsTooltip: () -> Unit,
    onDismissTooltip: (PlaylistTooltip) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val topTooltip = uiState.displayedTooltips.firstOrNull()

    Box(
        modifier = Modifier
            .background(MaterialTheme.theme.colors.primaryUi01)
            .fillMaxSize()
            .then(modifier),
    ) {
        Column {
            Toolbar(
                showActionButtons = !uiState.showEmptyState,
                onCreatePlaylist = onCreatePlaylist,
            )

            FreeAccountBanner(
                showBanner = uiState.showFreeAccountBanner,
                onFreeAccountBannerCtaClick = onFreeAccountBannerCtaClick,
                onFreeAccountBannerDismiss = onFreeAccountBannerDismiss,
            )

            PlaylistsContent(
                playlistsState = uiState.playlists,
                getArtworkUuidsFlow = getArtworkUuidsFlow,
                getEpisodeCountFlow = getEpisodeCountFlow,
                refreshArtworkUuids = refreshArtworkUuids,
                refreshEpisodeCount = refreshEpisodeCount,
                displayedTooltips = uiState.displayedTooltips,
                onDismissTooltip = onDismissTooltip,
                listState = listState,
                contentPadding = PaddingValues(
                    bottom = LocalDensity.current.run { uiState.miniPlayerInset.toDp() },
                ),
                onCreatePlaylist = onCreatePlaylist,
                onDeletePlaylist = onDeletePlaylist,
                onOpenPlaylist = onOpenPlaylist,
                onReorderPlaylists = onReorderPlaylists,
                onShowPlaylists = onShowPlaylists,
            )
        }
        if (topTooltip != null) {
            // We use a separate box to dismiss the tooltip instead of tooltip's 'onClickOutside'
            // in order to not dismiss the tooltip when the bottom navigation bar is tapped.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = null,
                        onClick = { onDismissTooltip(topTooltip) },
                    ),
            )

            LaunchedEffect(onShowPremadePlaylistsTooltip) {
                onShowPremadePlaylistsTooltip()
            }
        }
    }
}

@Composable
private fun PlaylistsContent(
    playlistsState: PlaylistsState,
    getArtworkUuidsFlow: (String) -> StateFlow<List<String>?>,
    getEpisodeCountFlow: (String) -> StateFlow<Int?>,
    refreshArtworkUuids: suspend (String) -> Unit,
    refreshEpisodeCount: suspend (String) -> Unit,
    displayedTooltips: List<PlaylistTooltip>,
    onDismissTooltip: (PlaylistTooltip) -> Unit,
    listState: LazyListState,
    contentPadding: PaddingValues,
    onCreatePlaylist: () -> Unit,
    onDeletePlaylist: (PlaylistPreview, settleRow: () -> Unit) -> Unit,
    onOpenPlaylist: (PlaylistPreview) -> Unit,
    onReorderPlaylists: (List<String>) -> Unit,
    onShowPlaylists: (List<PlaylistPreview>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    AnimatedContent(
        targetState = playlistsState,
        transitionSpec = { ContentTransitionSpec },
        contentKey = { playlists ->
            when (playlists) {
                is PlaylistsState.Loaded -> if (playlists.value.isNotEmpty()) "content" else "no_content"
                is PlaylistsState.Loading -> "loading"
            }
        },
        modifier = modifier,
    ) { targetState ->
        when (targetState) {
            is PlaylistsState.Loaded -> {
                val playlists = targetState.value
                if (playlists.isNotEmpty()) {
                    PlaylistsColumn(
                        playlists = playlists,
                        getArtworkUuidsFlow = getArtworkUuidsFlow,
                        getEpisodeCountFlow = getEpisodeCountFlow,
                        refreshArtworkUuids = refreshArtworkUuids,
                        refreshEpisodeCount = refreshEpisodeCount,
                        displayedTooltips = displayedTooltips,
                        onDismissTooltip = onDismissTooltip,
                        listState = listState,
                        contentPadding = contentPadding,
                        onDelete = { preview, draggableState ->
                            onDeletePlaylist(preview) {
                                scope.launch { draggableState.animateTo(SwipeToDeleteAnchor.Resting) }
                            }
                        },
                        onOpen = onOpenPlaylist,
                        onReorderPlaylists = onReorderPlaylists,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    NoPlaylistsContent(
                        onCreatePlaylist = onCreatePlaylist,
                        modifier = Modifier.padding(contentPadding),
                    )
                }
                LaunchedEffect(onShowPlaylists) {
                    onShowPlaylists(playlists)
                }
            }

            is PlaylistsState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun PlaylistsColumn(
    playlists: List<PlaylistPreview>,
    getArtworkUuidsFlow: (String) -> StateFlow<List<String>?>,
    getEpisodeCountFlow: (String) -> StateFlow<Int?>,
    refreshArtworkUuids: suspend (String) -> Unit,
    refreshEpisodeCount: suspend (String) -> Unit,
    displayedTooltips: List<PlaylistTooltip>,
    onDismissTooltip: (PlaylistTooltip) -> Unit,
    listState: LazyListState,
    contentPadding: PaddingValues,
    onDelete: (PlaylistPreview, AnchoredDraggableState<SwipeToDeleteAnchor>) -> Unit,
    onOpen: (PlaylistPreview) -> Unit,
    onReorderPlaylists: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val (displayItems, reorderableState) = rememberReorderableLazyListDataSource(
        listState = listState,
        items = playlists,
        itemKey = PlaylistPreview::uuid,
        onMove = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        },
        onCommit = { orderedList ->
            onReorderPlaylists(orderedList.map(PlaylistPreview::uuid))
        },
    )
    val baseColor = MaterialTheme.theme.colors.primaryUi01
    val highlightColor = MaterialTheme.theme.colors.primaryIcon01
    val draggedColor = remember(highlightColor, baseColor) {
        highlightColor.copy(alpha = 0.15f).compositeOver(baseColor.copy(alpha = 1f))
    }

    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = modifier,
    ) {
        val showPremadeTooltip = PlaylistTooltip.Premade in displayedTooltips
        val showRearrangeTooltip = PlaylistTooltip.Rearrange in displayedTooltips

        itemsIndexed(
            items = displayItems,
            key = { _, item -> item.uuid },
        ) { index, playlist ->
            ReorderableItem(reorderableState, key = playlist.uuid) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)
                val backgroundColor by animateColorAsState(if (isDragging) draggedColor else baseColor)

                PlaylistPreviewRow(
                    playlist = playlist,
                    getArtworkUuidsFlow = getArtworkUuidsFlow,
                    getEpisodeCountFlow = getEpisodeCountFlow,
                    refreshArtworkUuids = refreshArtworkUuids,
                    refreshEpisodeCount = refreshEpisodeCount,
                    showPremadeTooltip = showPremadeTooltip && index == displayItems.lastIndex,
                    showRearrangeTooltip = showRearrangeTooltip && index == 0,
                    onDismissTooltip = onDismissTooltip,
                    showDivider = index != displayItems.lastIndex,
                    backgroundColor = backgroundColor,
                    onDelete = { anchor -> onDelete(playlist, anchor) },
                    onClick = { onOpen(playlist) },
                    modifier = Modifier
                        .longPressDraggableHandle(
                            onDragStarted = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragStopped = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            },
                        )
                        .animateItem()
                        .shadow(elevation),
                )
            }
        }
    }
}

@Composable
private fun NoPlaylistsContent(
    onCreatePlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(
            modifier = Modifier.weight(1f),
        )
        NoContentBanner(
            title = stringResource(LR.string.playlists_empty_state_title),
            body = stringResource(LR.string.playlists_empty_state_body),
            iconResourceId = IR.drawable.ic_playlists,
            primaryButtonText = stringResource(LR.string.new_playlist),
            onPrimaryButtonClick = onCreatePlaylist,
        )

        Spacer(
            modifier = Modifier.weight(2f),
        )
    }
}

@Composable
private fun Toolbar(
    showActionButtons: Boolean,
    onCreatePlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ThemedTopAppBar(
        title = stringResource(LR.string.playlists),
        navigationButton = null,
        actions = {
            AnimatedVisibility(
                visible = showActionButtons,
                enter = FadeIn,
                exit = FadeOut,
            ) {
                IconButton(
                    onClick = onCreatePlaylist,
                ) {
                    Icon(
                        painter = painterResource(IR.drawable.ic_add_black_24dp),
                        contentDescription = stringResource(LR.string.new_playlist),
                        tint = MaterialTheme.theme.colors.secondaryIcon01,
                    )
                }
            }
        },
        windowInsets = WindowInsets.statusBars,
        modifier = modifier,
    )
}

@Composable
private fun ColumnScope.FreeAccountBanner(
    showBanner: Boolean,
    onFreeAccountBannerCtaClick: () -> Unit,
    onFreeAccountBannerDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = showBanner,
        enter = BannerEnterTransition,
        exit = BannerExitTransition,
        modifier = modifier,
    ) {
        Banner(
            title = stringResource(LR.string.encourage_account_playlists_banner_title),
            description = stringResource(LR.string.encourage_account_playlists_banner_description),
            actionLabel = stringResource(LR.string.encourage_account_banner_action_label),
            icon = painterResource(IR.drawable.ic_retry),
            onActionClick = onFreeAccountBannerCtaClick,
            onDismiss = onFreeAccountBannerDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

private val FadeIn = fadeIn()
private val FadeOut = fadeOut()
private val BannerEnterTransition = fadeIn() + expandVertically()
private val BannerExitTransition = fadeOut() + shrinkVertically()
private val ContentTransitionSpec = FadeIn togetherWith FadeOut

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun PlaylistsPageEmptyStatePreview() {
    AppThemeWithBackground(ThemeType.INDIGO) {
        PlaylistsPage(
            uiState = UiState(
                playlists = PlaylistsState.Loaded(value = emptyList()),
                showOnboarding = false,
                showFreeAccountBanner = true,
                displayedTooltips = emptyList(),
                miniPlayerInset = 0,
            ),
            getArtworkUuidsFlow = { MutableStateFlow(null) },
            getEpisodeCountFlow = { MutableStateFlow(null) },
            refreshArtworkUuids = {},
            refreshEpisodeCount = {},
            onCreatePlaylist = {},
            onDeletePlaylist = { _, _ -> },
            onOpenPlaylist = {},
            onReorderPlaylists = {},
            onShowPlaylists = {},
            onFreeAccountBannerCtaClick = {},
            onFreeAccountBannerDismiss = {},
            onShowPremadePlaylistsTooltip = {},
            onDismissTooltip = {},
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun PlaylistsPageEmptyStateNoBannerPreview() {
    AppThemeWithBackground(ThemeType.INDIGO) {
        PlaylistsPage(
            uiState = UiState(
                playlists = PlaylistsState.Loaded(value = emptyList()),
                showOnboarding = false,
                showFreeAccountBanner = false,
                displayedTooltips = emptyList(),
                miniPlayerInset = 0,
            ),
            getArtworkUuidsFlow = { MutableStateFlow(null) },
            getEpisodeCountFlow = { MutableStateFlow(null) },
            refreshArtworkUuids = {},
            refreshEpisodeCount = {},
            onCreatePlaylist = {},
            onDeletePlaylist = { _, _ -> },
            onOpenPlaylist = {},
            onReorderPlaylists = {},
            onShowPlaylists = {},
            onFreeAccountBannerCtaClick = {},
            onFreeAccountBannerDismiss = {},
            onShowPremadePlaylistsTooltip = {},
            onDismissTooltip = {},
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun PlaylistPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppThemeWithBackground(themeType) {
        PlaylistsPage(
            uiState = UiState(
                playlists = PlaylistsState.Loaded(
                    value = listOf(
                        ManualPlaylistPreview(
                            uuid = "uuid-0",
                            title = "Playlist 0",
                            settings = Playlist.Settings.ForPreview,
                            icon = PlaylistIcon(0),
                        ),
                        SmartPlaylistPreview(
                            uuid = "uuid-1",
                            title = "Playlist 1",
                            settings = Playlist.Settings.ForPreview,
                            smartRules = SmartRules.Default,
                            icon = PlaylistIcon(0),
                        ),
                    ),
                ),
                showOnboarding = false,
                showFreeAccountBanner = true,
                displayedTooltips = emptyList(),
                miniPlayerInset = 0,
            ),
            getArtworkUuidsFlow = { MutableStateFlow(null) },
            getEpisodeCountFlow = { MutableStateFlow(null) },
            refreshArtworkUuids = {},
            refreshEpisodeCount = {},
            onCreatePlaylist = {},
            onDeletePlaylist = { _, _ -> },
            onOpenPlaylist = {},
            onReorderPlaylists = {},
            onShowPlaylists = {},
            onFreeAccountBannerCtaClick = {},
            onFreeAccountBannerDismiss = {},
            onShowPremadePlaylistsTooltip = {},
            onDismissTooltip = {},
        )
    }
}
