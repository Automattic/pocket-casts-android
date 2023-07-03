package au.com.shiftyjelly.pocketcasts.wear.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import au.com.shiftyjelly.pocketcasts.R
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.wear.ui.component.MarqueeTextMediaDisplay
import au.com.shiftyjelly.pocketcasts.wear.ui.component.horologist.PodcastControlButtonsStyled
import au.com.shiftyjelly.pocketcasts.wear.ui.component.horologist.SetVolumeButtonStyled
import com.google.android.horologist.audio.ui.VolumeUiState
import com.google.android.horologist.compose.rotaryinput.onRotaryInputAccumulated
import com.google.android.horologist.media.ui.components.background.ColorBackground
import com.google.android.horologist.media.ui.components.display.MessageMediaDisplay
import com.google.android.horologist.media.ui.screens.player.PlayerScreen
import androidx.appcompat.R as AR
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object NowPlayingScreen {
    const val route = "now_playing"
    const val pagerIndex = 1
}

@OptIn(ExperimentalWearFoundationApi::class)
@Composable
fun NowPlayingScreen(
    modifier: Modifier = Modifier,
    playerViewModel: NowPlayingViewModel = hiltViewModel(),
    volumeViewModel: PCVolumeViewModel = hiltViewModel(),
    navigateToEpisode: (episodeUuid: String) -> Unit,
    navController: NavController,
) {

    // Listen for results from streaming confirmation screen
    navController.currentBackStackEntry?.savedStateHandle
        ?.getStateFlow<StreamingConfirmationScreen.Result?>(
            key = StreamingConfirmationScreen.resultKey,
            initialValue = null
        )
        ?.collectAsStateWithLifecycle()?.value?.let { streamingConfirmationResult ->

            LaunchedEffect(streamingConfirmationResult) {

                playerViewModel.onStreamingConfirmationResult(streamingConfirmationResult)

                // Clear result once consumed
                navController
                    .currentBackStackEntry
                    ?.savedStateHandle
                    ?.remove<StreamingConfirmationScreen.Result?>(
                        key = StreamingConfirmationScreen.resultKey
                    )
            }
        }

    val volumeUiState by volumeViewModel.volumeUiState.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier.fillMaxSize(),
    ) {

        val state = playerViewModel.state.collectAsState().value

        PlayerScreen(
            mediaDisplay = {
                when (state) {
                    NowPlayingViewModel.State.Loading -> {
                        MessageMediaDisplay(
                            message = stringResource(R.string.nothing_playing),
                            modifier = modifier
                        )
                    }

                    is NowPlayingViewModel.State.Loaded -> {
                        MarqueeTextMediaDisplay(
                            title = state.title,
                            artist = state.subtitle,
                            modifier = modifier
                                .clickable { navigateToEpisode(state.episodeUuid) },
                            isPlaybackError = state.error,
                        )
                    }

                    is NowPlayingViewModel.State.Empty -> {
                        MessageMediaDisplay(
                            message = stringResource(LR.string.empty_play_state),
                            modifier = modifier
                        )
                    }
                }
            },
            controlButtons = {
                when (state) {
                    is NowPlayingViewModel.State.Loaded -> {
                        PodcastControlButtonsStyled(
                            onPlayButtonClick = {
                                playerViewModel.onPlayButtonClick(
                                    showStreamingConfirmation = {
                                        navController.navigate(StreamingConfirmationScreen.route)
                                    }
                                )
                            },
                            onPauseButtonClick = playerViewModel::onPauseButtonClick,
                            playPauseButtonEnabled = true,
                            playing = state.playing,
                            trackPositionUiModel = state.trackPositionUiModel,
                            onSeekBackButtonClick = playerViewModel::onSeekBackButtonClick,
                            seekBackButtonEnabled = true,
                            onSeekForwardButtonClick = playerViewModel::onSeekForwardButtonClick,
                            seekForwardButtonEnabled = true,
                            seekBackIcon = ImageVector.vectorResource(au.com.shiftyjelly.pocketcasts.images.R.drawable.wear_skip_back),
                            seekForwardIcon = ImageVector.vectorResource(au.com.shiftyjelly.pocketcasts.images.R.drawable.wear_skip_foreward),
                            playIcon = ImageVector.vectorResource(IR.drawable.wear_play),
                            pauseIcon = ImageVector.vectorResource(IR.drawable.wear_pause),
                            seekIconSize = 35.dp,
                            seekIconAlign = Alignment.CenterHorizontally,
                            seekTapTargetSize = DpSize(50.dp, 60.dp),
                            progressColor = MaterialTheme.colors.onPrimary,
                            trackColor = MaterialTheme.colors.onPrimary.copy(alpha = 0.2f),
                            backgroundColor = Color.Transparent,
                            sidePadding = 12.dp,
                        )
                    }
                    is NowPlayingViewModel.State.Empty -> {
                        PodcastControlButtonsStyled(
                            onPlayButtonClick = {},
                            onPauseButtonClick = {},
                            playPauseButtonEnabled = false,
                            playing = false,
                            onSeekBackButtonClick = {},
                            seekBackButtonEnabled = false,
                            onSeekForwardButtonClick = {},
                            seekForwardButtonEnabled = false,
                            seekBackIcon = ImageVector.vectorResource(au.com.shiftyjelly.pocketcasts.images.R.drawable.wear_skip_back),
                            seekForwardIcon = ImageVector.vectorResource(au.com.shiftyjelly.pocketcasts.images.R.drawable.wear_skip_foreward),
                            playIcon = ImageVector.vectorResource(IR.drawable.wear_play),
                            pauseIcon = ImageVector.vectorResource(IR.drawable.wear_pause),
                            sidePadding = 12.dp,
                            seekIconAlign = Alignment.CenterHorizontally,
                        )
                    }
                    is NowPlayingViewModel.State.Loading -> {}
                }
            },
            buttons = {
                if (state is NowPlayingViewModel.State.Loaded) {
                    NowPlayingSettingsButtons(
                        volumeUiState = volumeUiState,
                        onVolumeClick = { navController.navigate(PCVolumeScreen.route) },
                        navController = navController,
                    )
                }
            },
            background = {
                when (state) {
                    NowPlayingViewModel.State.Loading -> Unit // Do Nothing
                    NowPlayingViewModel.State.Empty -> Unit

                    is NowPlayingViewModel.State.Loaded -> {
                        PodcastColorBackground(state)
                    }
                }
            },
            modifier = Modifier
                .onVolumeChangeByScroll(
                    focusRequester = rememberActiveFocusRequester(),
                    onVolumeChangeByScroll = volumeViewModel::onVolumeChangeByScroll
                )
        )
    }
}

@Composable
private fun PodcastColorBackground(
    state: NowPlayingViewModel.State.Loaded,
) {
    val context = LocalContext.current
    val tintColor = state.tintColor ?: context.getThemeColor(AR.attr.colorAccent)
    ColorBackground(
        color = Color(ThemeColor.podcastIcon02(state.theme.activeTheme, tintColor))
    )
}

@Composable
fun NowPlayingSettingsButtons(
    volumeUiState: VolumeUiState,
    onVolumeClick: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                navController.navigate(EffectsScreen.route)
            }
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_effects_off),
                contentDescription = stringResource(LR.string.player_effects),
                tint = Color.White
            )
        }
        SetVolumeButtonStyled(
            onVolumeClick = onVolumeClick,
            volumeUiState = volumeUiState,
            imageVolumeMute = ImageVector.vectorResource(IR.drawable.wear_volume_mute),
            imageVolume = ImageVector.vectorResource(IR.drawable.wear_volume),
            imageVolumeMax = ImageVector.vectorResource(IR.drawable.wear_volume_max),
        )
    }
}

@Composable
private fun Modifier.onVolumeChangeByScroll(
    focusRequester: FocusRequester,
    onVolumeChangeByScroll: (scrollPixels: Float) -> Unit,
) =
    onRotaryInputAccumulated(onValueChange = onVolumeChangeByScroll)
        .focusRequester(focusRequester)
        .focusable()
