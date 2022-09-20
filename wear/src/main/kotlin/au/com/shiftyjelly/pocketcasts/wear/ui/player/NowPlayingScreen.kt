package au.com.shiftyjelly.pocketcasts.wear.ui.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.Scaffold
import au.com.shiftyjelly.pocketcasts.wear.ui.component.AnimatedPlayerScreenMediaDisplay
import com.google.android.horologist.media.ui.components.PodcastControlButtons
import com.google.android.horologist.media.ui.screens.player.PlayerScreen
import com.google.android.horologist.media.ui.state.PlayerUiState
import com.google.android.horologist.media.ui.state.PlayerViewModel

object NowPlayingScreen {
    const val route = "now_playing_screen"
}

@Composable
fun NowPlayingScreen(
    modifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = hiltViewModel(),
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
    ) {
        PlayerScreen(
            playerViewModel = viewModel,
            mediaDisplay = { playerUiState ->
                AnimatedPlayerScreenMediaDisplay(playerUiState)
            },
            controlButtons = {
                PlayerScreenPodcastControlButtons(viewModel, it)
            }
        )
    }
}

@Composable
fun PlayerScreenPodcastControlButtons(
    playerViewModel: PlayerViewModel,
    playerUiState: PlayerUiState,
) {
    PodcastControlButtons(
        onPlayButtonClick = { playerViewModel.play() },
        onPauseButtonClick = { playerViewModel.pause() },
        playPauseButtonEnabled = playerUiState.playPauseEnabled,
        playing = playerUiState.playing,
        percent = playerUiState.trackPosition?.percent ?: 0f,
        onSeekBackButtonClick = { playerViewModel.seekBack() },
        seekBackButtonEnabled = playerUiState.seekBackEnabled,
        onSeekForwardButtonClick = { playerViewModel.seekForward() },
        seekForwardButtonEnabled = playerUiState.seekForwardEnabled,
        seekBackButtonIncrement = playerUiState.seekBackButtonIncrement,
        seekForwardButtonIncrement = playerUiState.seekForwardButtonIncrement
    )
}
