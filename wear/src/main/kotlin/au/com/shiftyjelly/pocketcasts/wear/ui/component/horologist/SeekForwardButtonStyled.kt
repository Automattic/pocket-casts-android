package au.com.shiftyjelly.pocketcasts.wear.ui.component.horologist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ButtonColors
import com.google.android.horologist.media.ui.R
import com.google.android.horologist.media.ui.components.controls.MediaButton
import com.google.android.horologist.media.ui.components.controls.MediaButtonDefaults
import com.google.android.horologist.media.ui.components.controls.SeekButtonIncrement

@Composable
fun SeekForwardButtonStyled(
    onClick: () -> Unit,
    seekButtonIncrement: SeekButtonIncrement,
    modifier: Modifier = Modifier,
    icon: ImageVector = MediaButtonDefaults.seekForwardIcon(seekButtonIncrement),
    enabled: Boolean = true,
    colors: ButtonColors = MediaButtonDefaults.mediaButtonDefaultColors,
    iconSize: Dp = 30.dp,
    iconAlign: Alignment.Horizontal = Alignment.Start,
    tapTargetSize: DpSize = DpSize(48.dp, 60.dp)
) {
    val contentDescription = when (seekButtonIncrement) {
        is SeekButtonIncrement.Known -> stringResource(
            id = R.string.horologist_seek_back_button_seconds_content_description,
            seekButtonIncrement.seconds
        )
        SeekButtonIncrement.Unknown -> stringResource(id = R.string.horologist_seek_back_button_content_description)
    }

    MediaButton(
        onClick = onClick,
        icon = icon,
        contentDescription = contentDescription,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        iconSize = iconSize,
        tapTargetSize = tapTargetSize,
        iconAlign = iconAlign
    )
}
