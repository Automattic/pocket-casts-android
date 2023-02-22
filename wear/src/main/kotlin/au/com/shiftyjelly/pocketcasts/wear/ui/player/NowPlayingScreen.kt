package au.com.shiftyjelly.pocketcasts.wear.ui.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.Scaffold
import au.com.shiftyjelly.pocketcasts.wear.ui.component.AnimatedPlayerScreenMediaDisplay
import com.google.android.horologist.audio.ui.ExperimentalHorologistAudioUiApi
import com.google.android.horologist.media.ui.components.PodcastControlButtons
import com.google.android.horologist.media.ui.screens.player.PlayerScreen
import com.google.android.horologist.media.ui.state.PlayerUiController
import com.google.android.horologist.media.ui.state.PlayerUiState

object NowPlayingScreen {
    const val argument = "playableUuid"
    const val route = "playable/{$argument}"
    fun navigateRoute(playableUuid: String) = "playable/$playableUuid"
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
        @OptIn(ExperimentalHorologistAudioUiApi::class)
        PlayerScreen(
            playerViewModel = playerViewModel,
            volumeViewModel = volumeViewModel,
            mediaDisplay = { playerUiState ->
                AnimatedPlayerScreenMediaDisplay(playerUiState)
            },
            controlButtons = { playerUiController, playerUiState ->
                PlayerScreenPodcastControlButtons(playerUiController, playerUiState)
            },
        )
    }
}

@Composable
fun PlayerScreenPodcastControlButtons(
    playerUiController: PlayerUiController,
    playerUiState: PlayerUiState,
) {
    PodcastControlButtons(
        onPlayButtonClick = { playerUiController.play() },
        onPauseButtonClick = { playerUiController.pause() },
        playPauseButtonEnabled = playerUiState.playPauseEnabled,
        playing = playerUiState.playing,
        trackPositionUiModel = playerUiState.trackPositionUiModel,
        onSeekBackButtonClick = { playerUiController.seekBack() },
        seekBackButtonEnabled = playerUiState.seekBackEnabled,
        onSeekForwardButtonClick = { playerUiController.seekForward() },
        seekForwardButtonEnabled = playerUiState.seekForwardEnabled,
        seekBackButtonIncrement = playerUiState.seekBackButtonIncrement,
        seekForwardButtonIncrement = playerUiState.seekForwardButtonIncrement
    )
}
