package au.com.shiftyjelly.pocketcasts.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import au.com.shiftyjelly.pocketcasts.component.LocalTvTopBarVisibility
import au.com.shiftyjelly.pocketcasts.component.TvTopBarVisibility
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.playlists.TvPlaylistsScreen
import au.com.shiftyjelly.pocketcasts.podcasts.TvYourPodcastsScreen
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.upnext.TvUpNextScreen

@Composable
fun TvScaffold(
    onLogIn: () -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TvScaffoldViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isProfileModalVisible by rememberSaveable { mutableStateOf(false) }
    val topBarVisibility = remember { TvTopBarVisibility() }

    CompositionLocalProvider(LocalTvTopBarVisibility provides topBarVisibility) {
        TvScaffoldContent(
            tabs = uiState.tabs,
            selectedTabIndex = uiState.selectedTabIndex,
            profile = uiState.profile,
            isTopBarVisible = topBarVisibility.isVisible,
            onTabSelect = viewModel::selectTab,
            onProfileClick = { isProfileModalVisible = true },
            modifier = modifier,
        ) { tab ->
            val navigateToHome = { viewModel.selectTab(TvTab.entries.indexOf(TvTab.Home)) }
            when (tab) {
                is TvTab.Home -> TvHomeScreen()

                is TvTab.YourPodcasts -> TvYourPodcastsScreen(
                    onNavigateToHome = navigateToHome,
                )

                is TvTab.Playlists -> TvPlaylistsScreen()

                is TvTab.UpNext -> TvUpNextScreen(
                    onNavigateToHome = navigateToHome,
                )

                else -> TvTabPlaceholder(tab = tab)
            }
        }

        if (isProfileModalVisible) {
            TvProfileModal(
                profile = uiState.profile,
                onDismissRequest = { isProfileModalVisible = false },
                onLogIn = {
                    isProfileModalVisible = false
                    onLogIn()
                },
                onCreateAccount = {
                    isProfileModalVisible = false
                    onCreateAccount()
                },
                // TODO: wire up the Starred Episodes and Listening History destinations.
                onStarredEpisodes = {},
                onListeningHistory = {},
                onLogOut = {
                    isProfileModalVisible = false
                    viewModel.signOut()
                },
            )
        }
    }
}

@Composable
private fun TvScaffoldContent(
    tabs: List<TvTab>,
    selectedTabIndex: Int,
    profile: TvProfileState,
    isTopBarVisible: Boolean,
    onTabSelect: (Int) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    tabContent: @Composable (TvTab) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        TvColors.DarkGray,
                        TvColors.Dark,
                    ),
                ),
            ),
    ) {
        AnimatedVisibility(
            visible = isTopBarVisible,
            enter = fadeIn() + expandVertically(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            TvTopBar(
                tabs = tabs,
                selectedTabIndex = selectedTabIndex,
                profile = profile,
                onTabSelect = onTabSelect,
                onProfileClick = onProfileClick,
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            val currentTab = tabs.getOrElse(selectedTabIndex) { tabs.first() }
            tabContent(currentTab)
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvScaffoldPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            var selectedIndex by remember { mutableIntStateOf(0) }
            TvScaffoldContent(
                tabs = TvTab.entries,
                selectedTabIndex = selectedIndex,
                profile = TvProfileState.SignedOut,
                isTopBarVisible = true,
                onTabSelect = { selectedIndex = it },
                onProfileClick = {},
            ) { tab ->
                TvTabPlaceholder(tab = tab)
            }
        }
    }
}
