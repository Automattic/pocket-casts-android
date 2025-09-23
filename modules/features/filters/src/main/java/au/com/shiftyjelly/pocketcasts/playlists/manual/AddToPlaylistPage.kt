package au.com.shiftyjelly.pocketcasts.playlists.manual

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.PreviewRegularDevice
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.FadedLazyColumn
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBanner
import au.com.shiftyjelly.pocketcasts.compose.components.PlaylistArtwork
import au.com.shiftyjelly.pocketcasts.compose.components.SearchBar
import au.com.shiftyjelly.pocketcasts.compose.components.SearchBarStyle
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.navigation.navigateOnce
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideInToEnd
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideInToStart
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideOutToEnd
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideOutToStart
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistNameInputField
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreviewForEpisode
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AddToPlaylistPage(
    playlistPreviews: List<PlaylistPreviewForEpisode>,
    unfilteredPlaylistsCount: Int,
    onClickCreatePlaylist: () -> Unit,
    onChangeEpisodeInPlaylist: (PlaylistPreviewForEpisode) -> Unit,
    onClickContinueWithNewPlaylist: () -> Unit,
    onClickDoneButton: () -> Unit,
    onClickNavigationButton: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    newPlaylistNameState: TextFieldState = rememberTextFieldState(),
    searchFieldState: TextFieldState = rememberTextFieldState(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val isTopPageDisplayed = backStackEntry == null || backStackEntry?.destination?.route == AddToPlaylistRoutes.HOME

    Column(
        modifier = modifier,
    ) {
        ThemedTopAppBar(
            navigationButton = NavigationButton.CloseBack(isClose = isTopPageDisplayed),
            title = if (isTopPageDisplayed) {
                stringResource(LR.string.add_to_playlist_description)
            } else {
                ""
            },
            style = ThemedTopAppBar.Style.Immersive,
            windowInsets = WindowInsets(0),
            onNavigationClick = onClickNavigationButton,
        )

        Spacer(
            modifier = Modifier.height(8.dp),
        )

        NavHost(
            navController = navController,
            startDestination = AddToPlaylistRoutes.HOME,
            enterTransition = { slideInToStart() },
            exitTransition = { slideOutToStart() },
            popEnterTransition = { slideInToEnd() },
            popExitTransition = { slideOutToEnd() },
            modifier = Modifier.weight(1f),
        ) {
            composable(AddToPlaylistRoutes.HOME) {
                SelectPlaylistsPage(
                    playlistPreviews = playlistPreviews,
                    unfilteredPlaylistsCount = unfilteredPlaylistsCount,
                    searchState = searchFieldState,
                    onCreatePlaylist = {
                        onClickContinueWithNewPlaylist()
                        navController.navigateOnce(AddToPlaylistRoutes.NEW_PLAYLIST)
                    },
                    onChangeEpisodeInPlaylist = onChangeEpisodeInPlaylist,
                    onClickDoneButton = onClickDoneButton,
                )
            }

            composable(AddToPlaylistRoutes.NEW_PLAYLIST) {
                NewPlaylistPage(
                    newPlaylistNameState = newPlaylistNameState,
                    onClickCreatePlaylist = onClickCreatePlaylist,
                )
            }
        }
    }
}

@Composable
private fun SelectPlaylistsPage(
    playlistPreviews: List<PlaylistPreviewForEpisode>,
    unfilteredPlaylistsCount: Int,
    searchState: TextFieldState,
    onCreatePlaylist: () -> Unit,
    onChangeEpisodeInPlaylist: (PlaylistPreviewForEpisode) -> Unit,
    onClickDoneButton: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding(),
    ) {
        // Wrapped in a column with scroll to support nested scroll interop
        // and allow dismissing the fragment by dragging top components
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            if (unfilteredPlaylistsCount != 0) {
                SearchBar(
                    state = searchState,
                    placeholder = stringResource(LR.string.add_to_playlist_find_playlist),
                    style = SearchBarStyle.Small,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                )
                Spacer(
                    modifier = Modifier.height(16.dp),
                )
            }
            NewPlaylistButton(
                onClick = onCreatePlaylist,
            )
        }
        PlaylistPreviewsColumn(
            playlistPreviews = playlistPreviews,
            unfilteredPlaylistsCount = unfilteredPlaylistsCount,
            onChangeEpisodeInPlaylist = onChangeEpisodeInPlaylist,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
        RowButton(
            text = stringResource(LR.string.done),
            onClick = onClickDoneButton,
        )
    }
}

@Composable
private fun NewPlaylistButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClick = onClick,
            )
            .padding(vertical = 12.dp, horizontal = 16.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.theme.colors.primaryUi05, RoundedCornerShape(4.dp)),
        ) {
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(2.dp)
                    .background(MaterialTheme.theme.colors.primaryIcon01, CircleShape),
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(16.dp)
                    .background(MaterialTheme.theme.colors.primaryIcon01, CircleShape),
            )
        }
        Spacer(
            modifier = Modifier.width(16.dp),
        )
        TextH40(
            text = stringResource(LR.string.new_playlist),
        )
    }
}

@Composable
private fun PlaylistPreviewsColumn(
    playlistPreviews: List<PlaylistPreviewForEpisode>,
    unfilteredPlaylistsCount: Int,
    onChangeEpisodeInPlaylist: (PlaylistPreviewForEpisode) -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(color = MaterialTheme.theme.colors.primaryIcon01),
    ) {
        val listState = rememberLazyListState()
        LaunchedEffect(playlistPreviews.map { it.uuid }) {
            listState.scrollToItem(0)
        }

        FadedLazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            state = listState,
            modifier = modifier,
        ) {
            itemsIndexed(
                items = playlistPreviews,
                key = { _, playlist -> playlist.uuid },
                contentType = { _, _ -> "playlist" },
            ) { index, playlist ->
                val contentDescription = if (playlist.canAddOrRemoveEpisode) {
                    if (playlist.hasEpisode) {
                        stringResource(LR.string.remove_from_playlist)
                    } else {
                        stringResource(LR.string.add_to_playlist_description)
                    }
                } else {
                    stringResource(LR.string.playlist_is_full_description)
                }
                PlaylistPreviewRow(
                    playlist = playlist,
                    showDivider = index != playlistPreviews.lastIndex,
                    modifier = Modifier
                        .clickable(
                            role = Role.Button,
                            onClick = { onChangeEpisodeInPlaylist(playlist) },
                        )
                        .semantics(mergeDescendants = true) {
                            this.contentDescription = contentDescription
                        },
                )
            }

            if (playlistPreviews.isEmpty() && unfilteredPlaylistsCount > 0) {
                item(
                    key = "no-content",
                    contentType = "no-content",
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                    ) {
                        NoContentBanner(
                            title = stringResource(LR.string.add_to_playlist_no_playlist_found_title),
                            body = stringResource(LR.string.add_to_playlist_no_playlist_found_body),
                            iconResourceId = IR.drawable.ic_exclamation_circle,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistPreviewRow(
    playlist: PlaylistPreviewForEpisode,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            val alpha = if (playlist.canAddOrRemoveEpisode) 1f else 0.4f
            PlaylistArtwork(
                podcastUuids = playlist.artworkPodcastUuids,
                artworkSize = 56.dp,
                elevation = if (playlist.canAddOrRemoveEpisode) 1.dp else 0.dp,
                modifier = Modifier.alpha(alpha),
            )
            Spacer(
                modifier = Modifier.width(16.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(alpha),
            ) {
                TextH40(
                    text = playlist.title,
                )
                TextP50(
                    text = pluralStringResource(LR.plurals.episode_count, playlist.episodeCount, playlist.episodeCount),
                    color = MaterialTheme.theme.colors.primaryText02,
                )
            }
            Spacer(
                modifier = Modifier.width(16.dp),
            )
            Checkbox(
                checked = playlist.hasEpisode,
                enabled = playlist.canAddOrRemoveEpisode,
                onCheckedChange = null,
                modifier = Modifier.alpha(alpha),
            )
        }
        if (showDivider) {
            HorizontalDivider(startIndent = 16.dp)
        }
    }
}

@Composable
private fun NewPlaylistPage(
    newPlaylistNameState: TextFieldState,
    onClickCreatePlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .imePadding()
            .navigationBarsPadding(),
    ) {
        TextH20(
            text = stringResource(LR.string.new_playlist),
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        PlaylistNameInputField(
            state = newPlaylistNameState,
            onClickImeAction = onClickCreatePlaylist,
            modifier = Modifier.focusRequester(focusRequester),
        )
        Spacer(
            modifier = Modifier.height(24.dp),
        )
        RowButton(
            text = stringResource(LR.string.create_playlist),
            enabled = newPlaylistNameState.text.isNotBlank(),
            onClick = onClickCreatePlaylist,
            includePadding = false,
        )
    }
}

private object AddToPlaylistRoutes {
    const val HOME = "home"
    const val NEW_PLAYLIST = "new_playlist"
}

@PreviewRegularDevice
@Composable
private fun AddToPlaylistPageEmptyStatePreview() {
    AppThemeWithBackground(ThemeType.LIGHT) {
        AddToPlaylistPage(
            playlistPreviews = emptyList(),
            unfilteredPlaylistsCount = 0,
            onClickCreatePlaylist = {},
            onChangeEpisodeInPlaylist = {},
            onClickContinueWithNewPlaylist = {},
            onClickDoneButton = {},
            onClickNavigationButton = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@PreviewRegularDevice
@Composable
private fun AddToPlaylistPageNoSearchPreview() {
    AppThemeWithBackground(ThemeType.LIGHT) {
        AddToPlaylistPage(
            playlistPreviews = emptyList(),
            unfilteredPlaylistsCount = 1,
            onClickCreatePlaylist = {},
            onChangeEpisodeInPlaylist = {},
            onClickContinueWithNewPlaylist = {},
            onClickDoneButton = {},
            onClickNavigationButton = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@PreviewRegularDevice
@Composable
private fun AddToPlaylistPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    val navController = rememberNavController()

    AppThemeWithBackground(themeType) {
        AddToPlaylistPage(
            playlistPreviews = listOf(
                PlaylistPreviewForEpisode(
                    uuid = "id-1",
                    title = "Playlist 1",
                    episodeCount = 99,
                    artworkPodcastUuids = emptyList(),
                    hasEpisode = false,
                    episodeLimit = 100,
                ),
                PlaylistPreviewForEpisode(
                    uuid = "id-2",
                    title = "Playlist 2",
                    episodeCount = 100,
                    artworkPodcastUuids = emptyList(),
                    hasEpisode = false,
                    episodeLimit = 100,
                ),
                PlaylistPreviewForEpisode(
                    uuid = "id-3",
                    title = "Playlist 3",
                    episodeCount = 100,
                    artworkPodcastUuids = emptyList(),
                    hasEpisode = true,
                    episodeLimit = 100,
                ),
                PlaylistPreviewForEpisode(
                    uuid = "id-4",
                    title = "Playlist 4",
                    episodeCount = 99,
                    artworkPodcastUuids = emptyList(),
                    hasEpisode = true,
                    episodeLimit = 100,
                ),
            ),
            unfilteredPlaylistsCount = 4,
            navController = navController,
            onClickCreatePlaylist = {},
            onChangeEpisodeInPlaylist = {},
            onClickContinueWithNewPlaylist = {},
            onClickDoneButton = {},
            onClickNavigationButton = navController::popBackStack,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
