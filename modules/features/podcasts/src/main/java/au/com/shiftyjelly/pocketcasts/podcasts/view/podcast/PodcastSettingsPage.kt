package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.FormField
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.navigation.navigateOnce
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideInToEnd
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideInToStart
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideOutToEnd
import au.com.shiftyjelly.pocketcasts.compose.navigation.slideOutToStart
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastSettingsViewModel
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.helper.ToolbarColors
import com.google.protobuf.value
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.StateFlow
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun PodcastSettingsPage(
    podcastTitle: String,
    toolbarColors: ToolbarColors,
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
                    getArtworkUuidsFlow = getArtworkUuidsFlow,
                    refreshArtworkUuids = refreshArtworkUuids,
                    onAddPodcastToPlaylists = onAddPodcastToPlaylists,
                    onRemovePodcastFromPlaylists = onRemovePodcastFromPlaylists,
                )
            }
        }
    }

    if (isSkipFirstDialogOpen) {
        ChaneSkipDurationDialog(
            title = stringResource(LR.string.podcast_settings_skip_first),
            placeholder = stringResource(LR.string.seconds_label),
            initialDuration = uiState?.podcast?.startFromSecs?.seconds ?: Duration.ZERO,
            onConfirm = { value ->
                onChangeSkipFirst(value)
                isSkipFirstDialogOpen = false
            },
            onDismiss = { isSkipFirstDialogOpen = false },
        )
    }

    if (isSkipLastDialogOpen) {
        ChaneSkipDurationDialog(
            title = stringResource(LR.string.podcast_settings_skip_last),
            placeholder = stringResource(LR.string.seconds_label),
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
private fun ChaneSkipDurationDialog(
    title: String,
    placeholder: String,
    initialDuration: Duration,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = onDismiss) {
        val textFieldState = rememberTextFieldState(initialText = initialDuration.inWholeSeconds.toString())
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(focusRequester) {
            focusRequester.requestFocus()
        }

        Card(
            modifier = modifier
                .wrapContentWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TextH30(
                    text = title,
                    modifier = Modifier.padding(16.dp),
                )
                FormField(
                    state = textFieldState,
                    placeholder = placeholder,
                    keyboardOptions = keyboardOptions,
                    onImeAction = { onConfirm(textFieldState.text.toString()) },
                    modifier = Modifier.focusRequester(focusRequester),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(stringResource(LR.string.cancel))
                    }
                    TextButton(
                        onClick = { onConfirm(textFieldState.text.toString()) },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(stringResource(LR.string.save))
                    }
                }
            }
        }
    }
}

private val keyboardOptions = KeyboardOptions(
    keyboardType = KeyboardType.Number,
    imeAction = ImeAction.Done,
)

private object PodcastSettingsRoutes {
    const val HOME = "home"
    const val ARCHIVE = "archive"
    const val EFFECTS = "effects"
    const val PLAYLISTS = "playlists"
}
