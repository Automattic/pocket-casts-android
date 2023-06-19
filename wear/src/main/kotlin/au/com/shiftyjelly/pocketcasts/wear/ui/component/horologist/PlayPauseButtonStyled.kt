package au.com.shiftyjelly.pocketcasts.wear.ui.component.horologist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ButtonColors
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import com.google.android.horologist.annotations.ExperimentalHorologistApi

@ExperimentalHorologistApi
@Composable
fun PlayPauseButtonStyled(
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    playing: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.iconButtonColors(),
    iconSize: Dp = 30.dp,
    tapTargetSize: DpSize = DpSize(60.dp, 60.dp),
    backgroundColor: Color = MaterialTheme.colors.onBackground.copy(alpha = 0.10f),
    playIcon: ImageVector = Icons.Default.PlayArrow,
    pauseIcon: ImageVector = Icons.Default.Pause,
    progress: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
            .size(tapTargetSize)
            .fillMaxSize()
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        progress()

        if (playing) {
            PauseButtonStyled(
                onClick = onPauseClick,
                enabled = enabled,
                modifier = modifier,
                colors = colors,
                iconSize = iconSize,
                tapTargetSize = tapTargetSize,
                icon = pauseIcon
            )
        } else {
            PlayButtonStyled(
                onClick = onPlayClick,
                enabled = enabled,
                modifier = modifier,
                colors = colors,
                iconSize = iconSize,
                tapTargetSize = tapTargetSize,
                icon = playIcon
            )
        }
    }
}
