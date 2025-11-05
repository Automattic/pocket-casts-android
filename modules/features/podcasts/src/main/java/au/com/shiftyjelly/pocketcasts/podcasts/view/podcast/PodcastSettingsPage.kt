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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.FormFieldDialog
import au.com.shiftyjelly.pocketcasts.compose.navigation.navigateOnce
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideInToEnd
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideInToStart
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideOutToEnd
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideOutToStart
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastSettingsViewModel
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.helper.ToolbarColors
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.StateFlow
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal data class SettingsColors(
    val toolbarColors: ToolbarColors,
    val iconColor: Color,
) {
    constructor(
        lightColor: Int,
        darkColor: Int,
        theme: Theme.ThemeType,
    ) : this(
        toolbarColors = ToolbarColors.podcast(lightColor, darkColor, theme),
        iconColor = Color(ThemeColor.podcastIcon02(theme, if (theme.darkTheme) darkColor else lightColor)),
    )
}

@Composable
internal fun PodcastSettingsPage(
    podcastTitle: String,
    colors: SettingsColors,
    uiState: PodcastSettingsViewModel.UiState?,
    getArtworkUuidsFlow: (String) -> StateFlow<List<String>?>,
    refreshArtworkUuids: suspend (String) -> Unit,
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
    onChangeSkipFirst: (String) -> Unit,
    onDecrementSkipLast: () -> Unit,
    onIncrementSkipLast: () -> Unit,
    onChangeSkipLast: (String) -> Unit,
    onAddPodcastToPlaylists: (List<String>) -> Unit,
    onRemovePodcastFromPlaylists: (List<String>) -> Unit,
    onUnfollow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    var isSkipFirstDialogOpen by remember { mutableStateOf(false) }
    var isSkipLastDialogOpen by remember { mutableStateOf(false) }

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
                        color = colors.toolbarColors.titleComposeColor,
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
            backgroundColor = colors.toolbarColors.backgroundComposeColor,
            iconColor = colors.toolbarColors.iconComposeColor,
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
                    iconTint = colors.iconColor,
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
                    onClickSkipFirst = {
                        if (!isSkipLastDialogOpen) {
                            isSkipFirstDialogOpen = true
                        }
                    },
                    onClickSkipLast = {
                        if (!isSkipFirstDialogOpen) {
                            isSkipLastDialogOpen = true
                        }
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
                    iconTint = colors.iconColor,
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
                    getArtworkUuidsFlow = getArtworkUuidsFlow,
                    refreshArtworkUuids = refreshArtworkUuids,
                    onAddPodcastToPlaylists = onAddPodcastToPlaylists,
                    onRemovePodcastFromPlaylists = onRemovePodcastFromPlaylists,
                )
            }
        }
    }

    if (isSkipFirstDialogOpen) {
        ChangeSkipDurationDialog(
            title = stringResource(LR.string.podcast_settings_skip_first),
            initialDuration = uiState?.podcast?.startFromSecs?.seconds ?: Duration.ZERO,
            onConfirm = { value ->
                onChangeSkipFirst(value)
                isSkipFirstDialogOpen = false
            },
            onDismiss = { isSkipFirstDialogOpen = false },
        )
    }

    if (isSkipLastDialogOpen) {
        ChangeSkipDurationDialog(
            title = stringResource(LR.string.podcast_settings_skip_last),
            initialDuration = uiState?.podcast?.skipLastSecs?.seconds ?: Duration.ZERO,
            onConfirm = { value ->
                onChangeSkipLast(value)
                isSkipLastDialogOpen = false
            },
            onDismiss = { isSkipLastDialogOpen = false },
        )
    }
}

@Composable
private fun ChangeSkipDurationDialog(
    title: String,
    initialDuration: Duration,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    FormFieldDialog(
        title = title,
        placeholder = stringResource(LR.string.seconds_label),
        initialValue = initialDuration.inWholeSeconds.toString(),
        keyboardType = KeyboardType.Number,
        onConfirm = onConfirm,
        onDismissRequest = onDismiss,
        isSaveEnabled = { value -> value.toIntOrNull()?.takeIf { it >= 0 } != null },
    )
}

private object PodcastSettingsRoutes {
    const val HOME = "home"
    const val ARCHIVE = "archive"
    const val EFFECTS = "effects"
    const val PLAYLISTS = "playlists"
}
