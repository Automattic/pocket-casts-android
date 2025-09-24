package au.com.shiftyjelly.pocketcasts.settings

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.AnimatedNonNullVisibility
import au.com.shiftyjelly.pocketcasts.compose.navigation.navigateOnce
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideInToEnd
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideInToStart
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideOutToEnd
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideOutToStart
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.AutoDownloadSettingsViewModel.UiState
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AutoDownloadSettingsPage(
    uiState: UiState?,
    onChangeUpNextDownload: (Boolean) -> Unit,
    onChangeNewEpisodesDownload: (Boolean) -> Unit,
    onChangeOnFollowDownload: (Boolean) -> Unit,
    onChangeAutoDownloadLimitSetting: () -> Unit,
    onChangeOnUnmeteredDownload: (Boolean) -> Unit,
    onChangeOnlyWhenChargingDownload: (Boolean) -> Unit,
    onStopAllDownloads: () -> Unit,
    onClearDownloadErrors: () -> Unit,
    onChangePodcast: (String, Boolean) -> Unit,
    onChangeAllPodcasts: (Boolean) -> Unit,
    onChangePlaylist: (String, Boolean) -> Unit,
    onChangeAllPlaylists: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = remember(backStackEntry) {
        backStackEntry?.destination?.route?.let(AutoDownloadSettingsRoute::fromValue) ?: AutoDownloadSettingsRoute.Home
    }
    val toolbarTitle = when (route) {
        AutoDownloadSettingsRoute.Home -> stringResource(LR.string.auto_download)
        AutoDownloadSettingsRoute.Podcasts -> stringResource(LR.string.settings_auto_download_podcasts)
        AutoDownloadSettingsRoute.Playlists -> if (FeatureFlag.isEnabled(Feature.PLAYLISTS_REBRANDING, immutable = true)) {
            stringResource(LR.string.settings_auto_download_playlists)
        } else {
            stringResource(LR.string.settings_auto_download_filters)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
    ) {
        ThemedTopAppBar(
            title = toolbarTitle,
            navigationButton = NavigationButton.Back,
            onNavigationClick = {
                if (!navController.popBackStack()) {
                    onDismiss()
                }
            },
            style = ThemedTopAppBar.Style.Solid,
            windowInsets = WindowInsets.statusBars,
        )

        AnimatedNonNullVisibility(
            item = uiState,
            enter = fadeIn,
            exit = fadeOut,
            modifier = Modifier.weight(1f),
        ) { state ->
            NavHost(
                navController = navController,
                startDestination = AutoDownloadSettingsRoute.Home.value,
                enterTransition = { slideInToStart() },
                exitTransition = { slideOutToStart() },
                popEnterTransition = { slideInToEnd() },
                popExitTransition = { slideOutToEnd() },
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(AutoDownloadSettingsRoute.Home.value) {
                    AutoDownloadSettingsHomePage(
                        uiState = state,
                        onChangeUpNextDownload = onChangeUpNextDownload,
                        onChangeNewEpisodesDownload = onChangeNewEpisodesDownload,
                        onChangePodcastsSetting = {
                            navController.navigateOnce(AutoDownloadSettingsRoute.Podcasts.value)
                        },
                        onChangeOnFollowDownload = onChangeOnFollowDownload,
                        onChangeAutoDownloadLimitSetting = onChangeAutoDownloadLimitSetting,
                        onChangePlaylistsSetting = {
                            navController.navigateOnce(AutoDownloadSettingsRoute.Playlists.value)
                        },
                        onChangeOnUnmeteredDownload = onChangeOnUnmeteredDownload,
                        onChangeOnlyWhenChargingDownload = onChangeOnlyWhenChargingDownload,
                        onStopAllDownloads = onStopAllDownloads,
                        onClearDownloadErrors = onClearDownloadErrors,
                    )
                }

                composable(AutoDownloadSettingsRoute.Podcasts.value) {
                    AutoDownloadSettingsPodcastsPage(
                        podcasts = state.podcasts,
                        onChangePodcast = onChangePodcast,
                        onChangeAllPodcasts = onChangeAllPodcasts,
                    )
                }

                composable(AutoDownloadSettingsRoute.Playlists.value) {
                    AutoDownloadSettingsPlaylistsPage(
                        playlists = state.playlists,
                        onChangePlaylist = onChangePlaylist,
                        onChangeAllPlaylists = onChangeAllPlaylists,
                    )
                }
            }
        }
    }
}

private val fadeIn = fadeIn()
private val fadeOut = fadeOut()

internal enum class AutoDownloadSettingsRoute(
    val value: String,
) {
    Home("home"),
    Podcasts("podcasts"),
    Playlists("playlists"),
    ;

    companion object {
        fun fromValue(value: String) = entries.firstOrNull { it.value == value }
    }
}
