package au.com.shiftyjelly.pocketcasts.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.component.LocalOpenNowPlaying
import au.com.shiftyjelly.pocketcasts.component.LocalTvTopBarVisibility
import au.com.shiftyjelly.pocketcasts.component.TvTopBarVisibility
import au.com.shiftyjelly.pocketcasts.playlists.TvPlaylistsScreen
import au.com.shiftyjelly.pocketcasts.podcasts.TvYourPodcastsScreen
import au.com.shiftyjelly.pocketcasts.theme.TvScreenBackgroundBrush
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.TvTopBarHeight
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
    var didFocusTopBar by rememberSaveable { mutableStateOf(false) }

    CompositionLocalProvider(
        LocalTvTopBarVisibility provides topBarVisibility,
        LocalOpenNowPlaying provides viewModel::openNowPlaying,
    ) {
        TvScaffoldContent(
            tabs = uiState.tabs,
            selectedTabIndex = uiState.selectedTabIndex,
            profile = uiState.profile,
            isTopBarVisible = topBarVisibility.isVisible,
            autoFocusSelectedTab = !didFocusTopBar,
            onSelectedTabFocus = { didFocusTopBar = true },
            onTabSelect = { index -> uiState.tabs.getOrNull(index)?.let(viewModel::selectTab) },
            onProfileClick = { isProfileModalVisible = true },
            modifier = modifier,
        ) { tab ->
            val navigateToHome = { viewModel.selectTab(TvTab.Home) }
            // Tabs without a detail screen sit below the bar; the detail-bearing tabs pad their own
            // content so their overlays can fill the full height.
            val belowTopBar = Modifier.fillMaxSize().padding(top = TvTopBarHeight)
            when (tab) {
                is TvTab.Home -> TvHomeScreen()

                is TvTab.YourPodcasts -> TvYourPodcastsScreen(
                    onNavigateToHome = navigateToHome,
                )

                is TvTab.Playlists -> TvPlaylistsScreen()

                is TvTab.UpNext -> Box(modifier = belowTopBar) {
                    TvUpNextScreen(onNavigateToHome = navigateToHome)
                }

                else -> Box(modifier = belowTopBar) {
                    TvTabPlaceholder(tab = tab)
                }
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
    autoFocusSelectedTab: Boolean = true,
    onSelectedTabFocus: () -> Unit = {},
    tabContent: @Composable (TvTab) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvScreenBackgroundBrush),
    ) {
        val currentTab = tabs.getOrElse(selectedTabIndex) { tabs.first() }
        // Tab content fills the whole area; top-level content reserves TvTopBarHeight for the bar,
        // while detail overlays fill the full height under the hidden bar.
        Crossfade(
            targetState = currentTab,
            animationSpec = tween(durationMillis = TAB_CONTENT_ANIMATION_MILLIS, easing = FastOutSlowInEasing),
            label = "TvTabContent",
            modifier = Modifier.fillMaxSize(),
        ) { tab ->
            tabContent(tab)
        }

        AnimatedVisibility(
            visible = isTopBarVisible,
            enter = fadeIn(tween(durationMillis = TOP_BAR_ANIMATION_MILLIS, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(durationMillis = TOP_BAR_ANIMATION_MILLIS, easing = FastOutSlowInEasing)),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            TvTopBar(
                tabs = tabs,
                selectedTabIndex = selectedTabIndex,
                profile = profile,
                onTabSelect = onTabSelect,
                onProfileClick = onProfileClick,
                autoFocusSelectedTab = autoFocusSelectedTab,
                onSelectedTabFocus = onSelectedTabFocus,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private const val TOP_BAR_ANIMATION_MILLIS = 300
private const val TAB_CONTENT_ANIMATION_MILLIS = 300

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvScaffoldPreview() {
    TvTheme {
        var selectedIndex by remember { mutableIntStateOf(0) }
        TvScaffoldContent(
            tabs = TvTab.entries,
            selectedTabIndex = selectedIndex,
            profile = TvProfileState.SignedOut,
            isTopBarVisible = true,
            onTabSelect = { selectedIndex = it },
            onProfileClick = {},
        ) { tab ->
            Box(modifier = Modifier.fillMaxSize().padding(top = TvTopBarHeight)) {
                TvTabPlaceholder(tab = tab)
            }
        }
    }
}
