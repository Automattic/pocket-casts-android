package au.com.shiftyjelly.pocketcasts.wear.ui.component.horologist

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ButtonColors
import androidx.wear.compose.material.MaterialTheme
import com.google.android.horologist.media.ui.components.controls.MediaButtonDefaults
import com.google.android.horologist.media.ui.components.controls.SeekButtonIncrement
import com.google.android.horologist.media.ui.state.model.TrackPositionUiModel

@Composable
fun PodcastControlButtonsStyled(
    onPlayButtonClick: () -> Unit,
    onPauseButtonClick: () -> Unit,
    playPauseButtonEnabled: Boolean,
    playing: Boolean,
    onSeekBackButtonClick: () -> Unit,
    seekBackButtonEnabled: Boolean,
    onSeekForwardButtonClick: () -> Unit,
    seekForwardButtonEnabled: Boolean,
    trackPositionUiModel: TrackPositionUiModel,
    modifier: Modifier = Modifier,
    seekBackButtonIncrement: SeekButtonIncrement = SeekButtonIncrement.Unknown,
    seekForwardButtonIncrement: SeekButtonIncrement = SeekButtonIncrement.Unknown,
    colors: ButtonColors = MediaButtonDefaults.mediaButtonDefaultColors,
    seekBackIcon: ImageVector = MediaButtonDefaults.seekBackIcon(seekBackButtonIncrement),
    seekForwardIcon: ImageVector = MediaButtonDefaults.seekForwardIcon(seekForwardButtonIncrement),
    playIcon: ImageVector = Icons.Default.PlayArrow,
    pauseIcon: ImageVector = Icons.Default.Pause,
    seekIconSize: Dp = 30.dp,
    seekIconAlign: Alignment.Horizontal = Alignment.Start,
    seekTapTargetSize: DpSize = DpSize(48.dp, 60.dp),
    progressColor: Color = MaterialTheme.colors.primary,
    trackColor: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.10f),
    backgroundColor: Color = MaterialTheme.colors.onBackground.copy(alpha = 0.10f),
    sidePadding: Dp = 17.dp,
) {
    ControlButtonLayoutStyled(
        modifier = modifier,
        sidePadding = sidePadding,
        leftButton = {
            SeekForwardButtonStyled(
                onClick = onSeekBackButtonClick,
                seekButtonIncrement = seekBackButtonIncrement,
                icon = seekBackIcon,
                iconSize = seekIconSize,
                iconAlign = seekIconAlign,
                tapTargetSize = seekTapTargetSize,
                colors = colors,
                enabled = seekBackButtonEnabled
            )
        },
        middleButton = {
            PlayPauseProgressButtonStyled(
                onPlayClick = onPlayButtonClick,
                onPauseClick = onPauseButtonClick,
                enabled = playPauseButtonEnabled,
                playing = playing,
                trackPositionUiModel = trackPositionUiModel,
                colors = colors,
                progressColor = progressColor,
                trackColor = trackColor,
                backgroundColor = backgroundColor,
                playIcon = playIcon,
                pauseIcon = pauseIcon,
            )
        },
        rightButton = {
            SeekForwardButtonStyled(
                onClick = onSeekForwardButtonClick,
                seekButtonIncrement = seekForwardButtonIncrement,
                icon = seekForwardIcon,
                iconSize = seekIconSize,
                iconAlign = seekIconAlign,
                tapTargetSize = seekTapTargetSize,
                colors = colors,
                enabled = seekForwardButtonEnabled
            )
        }
    )
}
