package au.com.shiftyjelly.pocketcasts.wear.ui.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.R
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.wear.ui.component.MarqueeTextMediaDisplay
import com.google.android.horologist.media.ui.components.PodcastControlButtons
import com.google.android.horologist.media.ui.components.display.MessageMediaDisplay
import com.google.android.horologist.media.ui.screens.player.PlayerScreen

object NowPlayingScreen {
    const val route = "now_playing"
}

@Composable
fun NowPlayingScreen(
    modifier: Modifier = Modifier,
    playerViewModel: NowPlayingViewModel = hiltViewModel(),
    volumeViewModel: PCVolumeViewModel = hiltViewModel(),
) {
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
                            modifier = modifier,
                            title = state.title,
                            artist = state.subtitle,
                        )
                    }
                }
            },
            controlButtons = {
                if (state is NowPlayingViewModel.State.Loaded) {
                    PodcastControlButtons(
                        onPlayButtonClick = playerViewModel::onPlayButtonClick,
                        onPauseButtonClick = playerViewModel::onPauseButtonClick,
                        playPauseButtonEnabled = true,
                        playing = state.playing,
                        trackPositionUiModel = state.trackPositionUiModel,
                        onSeekBackButtonClick = playerViewModel::onSeekBackButtonClick,
                        seekBackButtonEnabled = true,
                        onSeekForwardButtonClick = playerViewModel::onSeekForwardButtonClick,
                        seekForwardButtonEnabled = true,
                        seekBackButtonIncrement = state.seekBackwardIncrement,
                        seekForwardButtonIncrement = state.seekForwardIncrement,
                    )
                }
            },
            buttons = {
                if (state is NowPlayingViewModel.State.Loaded) {
                    val position = TimeHelper.formattedSeconds(
                        state.trackPositionUiModel.position.inWholeSeconds.toDouble()
                    )
                    val duration = TimeHelper.formattedSeconds(
                        state.trackPositionUiModel.duration.inWholeSeconds.toDouble()
                    )
                    Text(
                        text = "$position / $duration",
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(alignment = Alignment.CenterVertically)
                    )
                }
            },
            modifier = Modifier,
            background = {},
            onVolumeChangeByScroll = volumeViewModel::onVolumeChangeByScroll,
        )
    }
}
