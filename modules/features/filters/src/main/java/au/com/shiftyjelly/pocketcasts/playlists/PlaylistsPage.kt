package au.com.shiftyjelly.pocketcasts.playlists

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
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
import au.com.shiftyjelly.pocketcasts.playlists.PlaylistsViewModel.PlaylistsState
import au.com.shiftyjelly.pocketcasts.playlists.PlaylistsViewModel.UiState
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreview
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import sh.calvin.reorderable.ReorderableItem
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun PlaylistsPage(
    uiState: UiState,
    onCreatePlaylist: () -> Unit,
    onDeletePlaylist: (PlaylistPreview) -> Unit,
    onReorderPlaylists: (List<String>) -> Unit,
    onShowOptions: () -> Unit,
    onFreeAccountBannerCtaClick: () -> Unit,
    onFreeAccountBannerDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.theme.colors.primaryUi01)
            .fillMaxSize()
            .then(modifier),
    ) {
        Toolbar(
            showActionButtons = !uiState.showEmptyState,
            onCreatePlaylist = onCreatePlaylist,
            onShowOptions = onShowOptions,
        )

        FreeAccountBanner(
            showBanner = uiState.showFreeAccountBanner,
            onFreeAccountBannerCtaClick = onFreeAccountBannerCtaClick,
            onFreeAccountBannerDismiss = onFreeAccountBannerDismiss,
        )

        PlaylistsContent(
            playlistsState = uiState.playlists,
            listState = listState,
            contentPadding = PaddingValues(
                bottom = LocalDensity.current.run { uiState.miniPlayerInset.toDp() },
            ),
            onCreatePlaylist = onCreatePlaylist,
            onDeletePlaylist = onDeletePlaylist,
            onReorderPlaylists = onReorderPlaylists,
        )
    }
}

@Composable
private fun ColumnScope.PlaylistsContent(
    playlistsState: PlaylistsState,
    listState: LazyListState,
    contentPadding: PaddingValues,
    onCreatePlaylist: () -> Unit,
    onDeletePlaylist: (PlaylistPreview) -> Unit,
    onReorderPlaylists: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
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
    ) { playlists ->
        when (playlists) {
            is PlaylistsState.Loaded -> if (playlists.value.isNotEmpty()) {
                PlaylistsColumn(
                    playlists = playlists.value,
                    listState = listState,
                    contentPadding = contentPadding,
                    onDelete = onDeletePlaylist,
                    onReorderPlaylists = onReorderPlaylists,
                )
            } else {
                NoPlaylistsContent(
                    onCreatePlaylist = onCreatePlaylist,
                    modifier = Modifier.padding(contentPadding),
                )
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
    listState: LazyListState,
    contentPadding: PaddingValues,
    onDelete: (PlaylistPreview) -> Unit,
    onReorderPlaylists: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (displayItems, reorderableState) = rememberReorderableLazyListDataSource(
        listState = listState,
        items = playlists,
        itemKey = PlaylistPreview::uuid,
        onCommit = { orderedList ->
            onReorderPlaylists(orderedList.map(PlaylistPreview::uuid))
        },
    )

    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = modifier,
    ) {
        itemsIndexed(
            items = displayItems,
            key = { _, item -> item.uuid },
        ) { index, playlist ->
            ReorderableItem(reorderableState, key = playlist.uuid) {
                PlaylistPreviewRow(
                    playlist = playlist,
                    showDivider = index != displayItems.lastIndex,
                    onDelete = { onDelete(playlist) },
                    modifier = Modifier
                        .longPressDraggableHandle()
                        .animateItem(),
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.NoPlaylistsContent(
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
    onShowOptions: () -> Unit,
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

            AnimatedVisibility(
                visible = showActionButtons,
                enter = FadeIn,
                exit = FadeOut,
            ) {
                IconButton(
                    onClick = onShowOptions,
                ) {
                    Icon(
                        painter = painterResource(IR.drawable.ic_overflow),
                        contentDescription = stringResource(LR.string.options),
                        tint = MaterialTheme.theme.colors.secondaryIcon01,
                    )
                }
            }
        },
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
            title = stringResource(LR.string.encourage_account_filters_banner_title),
            description = stringResource(LR.string.encourage_account_filters_banner_description),
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
                miniPlayerInset = 0,
            ),
            onCreatePlaylist = {},
            onDeletePlaylist = {},
            onShowOptions = {},
            onFreeAccountBannerCtaClick = {},
            onFreeAccountBannerDismiss = {},
            onReorderPlaylists = {},
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
                miniPlayerInset = 0,
            ),
            onCreatePlaylist = {},
            onDeletePlaylist = {},
            onShowOptions = {},
            onFreeAccountBannerCtaClick = {},
            onFreeAccountBannerDismiss = {},
            onReorderPlaylists = {},
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
                    value = List(3) { index ->
                        PlaylistPreview(
                            uuid = "uuid-$index",
                            title = "Playlist $index",
                            episodeCount = index,
                            podcasts = emptyList(),
                        )
                    },
                ),
                showOnboarding = false,
                showFreeAccountBanner = true,
                miniPlayerInset = 0,
            ),
            onCreatePlaylist = {},
            onDeletePlaylist = {},
            onShowOptions = {},
            onFreeAccountBannerCtaClick = {},
            onFreeAccountBannerDismiss = {},
            onReorderPlaylists = {},
        )
    }
}
