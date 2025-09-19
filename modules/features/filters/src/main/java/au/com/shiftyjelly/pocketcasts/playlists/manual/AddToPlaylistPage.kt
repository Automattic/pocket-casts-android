package au.com.shiftyjelly.pocketcasts.playlists.manual

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import au.com.shiftyjelly.pocketcasts.compose.components.SearchBar
import au.com.shiftyjelly.pocketcasts.compose.components.SearchBarStyle
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.navigation.navigateOnce
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideInToEnd
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideInToStart
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideOutToEnd
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideOutToStart
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistNameInputField
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AddToPlaylistPage(
    onClickDoneButton: () -> Unit,
    onClickCreatePlaylist: () -> Unit,
    onClickNavigationButton: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    newPlaylistNameState: TextFieldState = rememberTextFieldState(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val isTopPageDisplayed = backStackEntry == null || backStackEntry?.destination?.route == AddEpisodesRoutes.HOME

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
                    searchState = rememberTextFieldState(),
                    onCreatePlaylist = { navController.navigateOnce(AddToPlaylistRoutes.NEW_PLAYLIST) },
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
    searchState: TextFieldState,
    onCreatePlaylist: () -> Unit,
    onClickDoneButton: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding(),
    ) {
        SearchBar(
            state = searchState,
            placeholder = stringResource(LR.string.search),
            style = SearchBarStyle.Small,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        NewPlaylistButton(
            onClick = onCreatePlaylist,
        )
        Spacer(
            modifier = Modifier.weight(1f),
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
private fun AddToPlaylistPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    val navController = rememberNavController()

    AppThemeWithBackground(themeType) {
        AddToPlaylistPage(
            navController = navController,
            onClickDoneButton = {},
            onClickCreatePlaylist = {},
            onClickNavigationButton = navController::popBackStack,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
