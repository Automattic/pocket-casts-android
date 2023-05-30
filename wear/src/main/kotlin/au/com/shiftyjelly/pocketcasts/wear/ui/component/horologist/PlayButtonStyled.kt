package au.com.shiftyjelly.pocketcasts.wear.ui.component.horologist

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
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

@Composable
fun PlayButtonStyled(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = MediaButtonDefaults.mediaButtonDefaultColors,
    iconSize: Dp = 30.dp,
    icon: ImageVector = Icons.Default.PlayArrow,
    tapTargetSize: DpSize = DpSize(60.dp, 60.dp)
) {
    MediaButton(
        onClick = onClick,
        icon = icon,
        contentDescription = stringResource(id = R.string.horologist_play_button_content_description),
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        iconSize = iconSize,
        tapTargetSize = tapTargetSize
    )
}
