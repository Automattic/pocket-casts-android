package au.com.shiftyjelly.pocketcasts.wear.ui.component.horologist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ButtonColors
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import com.google.android.horologist.media.ui.state.model.TrackPositionUiModel

@Composable
fun PlayPauseProgressButtonStyled(
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    playing: Boolean,
    trackPositionUiModel: TrackPositionUiModel,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.iconButtonColors(),
    iconSize: Dp = 30.dp,
    tapTargetSize: DpSize = DpSize(60.dp, 60.dp),
    progressStrokeWidth: Dp = 4.dp,
    progressColor: Color = MaterialTheme.colors.primary,
    trackColor: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.10f),
    backgroundColor: Color = MaterialTheme.colors.onBackground.copy(alpha = 0.10f),
    playIcon: ImageVector = Icons.Default.PlayArrow,
    pauseIcon: ImageVector = Icons.Default.Pause,
) {
    PlayPauseButtonStyled(
        onPlayClick = onPlayClick,
        onPauseClick = onPauseClick,
        enabled = enabled,
        playing = playing,
        modifier = modifier,
        colors = colors,
        iconSize = iconSize,
        tapTargetSize = tapTargetSize,
        backgroundColor = backgroundColor,
        playIcon = playIcon,
        pauseIcon = pauseIcon,
    ) {
        val progress by ProgressStateHolderStyled.fromTrackPositionUiModel(trackPositionUiModel)
        if (trackPositionUiModel.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                indicatorColor = progressColor,
                trackColor = trackColor,
                strokeWidth = progressStrokeWidth
            )
        } else if (trackPositionUiModel.showProgress) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                progress = progress,
                indicatorColor = progressColor,
                trackColor = trackColor,
                strokeWidth = progressStrokeWidth
            )
        }
    }
}
