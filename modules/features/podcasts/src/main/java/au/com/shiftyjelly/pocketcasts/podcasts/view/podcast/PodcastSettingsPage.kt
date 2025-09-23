package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.navigation.navigateOnce
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideInToEnd
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideInToStart
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideOutToEnd
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideOutToStart
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveAfterPlaying
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveInactive
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveLimit
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastSettingsViewModel
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.helper.ToolbarColors
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun PodcastSettingsPage(
    podcastTitle: String,
    toolbarColors: ToolbarColors,
    uiState: PodcastSettingsViewModel.UiState?,
    onChangeNotifications: (Boolean) -> Unit,
    onChangeAutoDownload: (Boolean) -> Unit,
    onChangeAddToUpNext: (Boolean) -> Unit,
    onChangeUpNextPosition: () -> Unit,
    onChangeUpNextGlobalSettings: () -> Unit,
    onChangeAutoArchive: (Boolean) -> Unit,
    onChangeAutoArchiveAfterPlayingSetting: () -> Unit,
    onChangeAutoArchiveAfterInactiveSetting: () -> Unit,
    onChangeAutoArchiveLimitSetting: () -> Unit,
    onChangePlaybackEffects: (Boolean) -> Unit,
    onDecrementPlaybackSpeed: () -> Unit,
    onIncrementPlaybackSpeed: () -> Unit,
    onChangeTrimMode: (Boolean) -> Unit,
    onChangeTrimModeSetting: () -> Unit,
    onChangeVolumeBoost: (Boolean) -> Unit,
    onDecrementSkipFirst: () -> Unit,
    onIncrementSkipFirst: () -> Unit,
    onDecrementSkipLast: () -> Unit,
    onIncrementSkipLast: () -> Unit,
    onAddPodcastToPlaylists: (List<String>) -> Unit,
    onRemovePodcastFromPlaylists: (List<String>) -> Unit,
    onUnfollow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val toolbarTitle = when (backStackEntry?.destination?.route) {
        PodcastSettingsRoutes.HOME, null -> podcastTitle
        PodcastSettingsRoutes.ARCHIVE -> stringResource(LR.string.podcast_settings_auto_archive)
        PodcastSettingsRoutes.EFFECTS -> stringResource(LR.string.podcast_playback_effects)
        PodcastSettingsRoutes.PLAYLISTS -> if (FeatureFlag.isEnabled(Feature.PLAYLISTS_REBRANDING, immutable = true)) {
            stringResource(LR.string.select_smart_playlists)
        } else {
            stringResource(LR.string.settings_select_filters)
        }

        else -> podcastTitle
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.theme.colors.primaryUi02)
            .fillMaxSize()
            .then(modifier),
    ) {
        ThemedTopAppBar(
            title = {
                Crossfade(toolbarTitle) { title ->
                    Text(
                        text = title,
                        color = toolbarColors.titleComposeColor,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            navigationButton = NavigationButton.Back,
            onNavigationClick = {
                if (!navController.popBackStack()) {
                    onDismiss()
                }
            },
            style = ThemedTopAppBar.Style.Immersive,
            backgroundColor = toolbarColors.backgroundComposeColor,
            iconColor = toolbarColors.iconComposeColor,
            windowInsets = WindowInsets.statusBars,
        )

        NavHost(
            navController = navController,
            startDestination = PodcastSettingsRoutes.HOME,
            enterTransition = { slideInToStart() },
            exitTransition = { slideOutToStart() },
            popEnterTransition = { slideInToEnd() },
            popExitTransition = { slideOutToEnd() },
            modifier = Modifier.weight(1f),
        ) {
            composable(PodcastSettingsRoutes.HOME) {
                if (uiState == null) {
                    return@composable
                }
                PodcastSettingsHomePage(
                    uiState = uiState,
                    toolbarColors = toolbarColors,
                    onChangeNotifications = onChangeNotifications,
                    onChangeAutoDownload = onChangeAutoDownload,
                    onChangeAddToUpNext = onChangeAddToUpNext,
                    onChangeUpNextPosition = onChangeUpNextPosition,
                    onChangeUpNextGlobalSettings = onChangeUpNextGlobalSettings,
                    onChangeAutoArchiveSettings = {
                        navController.navigateOnce(PodcastSettingsRoutes.ARCHIVE)
                    },
                    onChangePlaybackEffectsSettings = {
                        navController.navigateOnce(PodcastSettingsRoutes.EFFECTS)
                    },
                    onDecrementSkipFirst = onDecrementSkipFirst,
                    onIncrementSkipFirst = onIncrementSkipFirst,
                    onDecrementSkipLast = onDecrementSkipLast,
                    onIncrementSkipLast = onIncrementSkipLast,
                    onChangePlaylistSettings = {
                        navController.navigateOnce(PodcastSettingsRoutes.PLAYLISTS)
                    },
                    onUnfollow = onUnfollow,
                )
            }

            composable(PodcastSettingsRoutes.ARCHIVE) {
                if (uiState == null) {
                    return@composable
                }
                PodcastSettingsArchivePage(
                    podcast = uiState.podcast,
                    onChangeAutoArchive = onChangeAutoArchive,
                    onChangeAutoArchiveAfterPlayingSetting = onChangeAutoArchiveAfterPlayingSetting,
                    onChangeAutoArchiveAfterInactiveSetting = onChangeAutoArchiveAfterInactiveSetting,
                    onChangeAutoArchiveLimitSetting = onChangeAutoArchiveLimitSetting,
                )
            }

            composable(PodcastSettingsRoutes.EFFECTS) {
                if (uiState == null) {
                    return@composable
                }
                PodcastSettingsEffectsPage(
                    podcast = uiState.podcast,
                    toolbarColors = toolbarColors,
                    onChangePlaybackEffects = onChangePlaybackEffects,
                    onDecrementSpeed = onDecrementPlaybackSpeed,
                    onIncrementSpeed = onIncrementPlaybackSpeed,
                    onChangeTrimMode = onChangeTrimMode,
                    onChangeTrimModeSetting = onChangeTrimModeSetting,
                    onChangeVolumeBoost = onChangeVolumeBoost,
                )
            }

            composable(PodcastSettingsRoutes.PLAYLISTS) {
                if (uiState == null) {
                    return@composable
                }
                PodcastSettingsPlaylistsPage(
                    uiState = uiState,
                    onAddPodcastToPlaylists = onAddPodcastToPlaylists,
                    onRemovePodcastFromPlaylists = onRemovePodcastFromPlaylists,
                )
            }
        }
    }
}

private object PodcastSettingsRoutes {
    const val HOME = "home"
    const val ARCHIVE = "archive"
    const val EFFECTS = "effects"
    const val PLAYLISTS = "playlists"
}
