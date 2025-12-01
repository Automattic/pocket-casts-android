package au.com.shiftyjelly.pocketcasts.wear.ui.component.horologist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ButtonColors
import com.google.android.horologist.media.ui.components.controls.MediaButton
import com.google.android.horologist.media.ui.components.controls.MediaButtonDefaults
import com.google.android.horologist.media.ui.components.controls.SeekButtonIncrement
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun SeekForwardButtonStyled(
    onClick: () -> Unit,
    seekButtonIncrement: SeekButtonIncrement,
    skipForward: Boolean,
    modifier: Modifier = Modifier,
    icon: ImageVector = MediaButtonDefaults.seekForwardIcon(seekButtonIncrement),
    enabled: Boolean = true,
    colors: ButtonColors = MediaButtonDefaults.mediaButtonDefaultColors,
    iconSize: Dp = 30.dp,
    iconAlign: Alignment.Horizontal = Alignment.Start,
) {
    val contentDescription = when (seekButtonIncrement) {
        is SeekButtonIncrement.Known -> stringResource(
            id = if (skipForward) LR.string.player_notification_skip_forward_seconds else LR.string.player_notification_skip_back_seconds,
            seekButtonIncrement.seconds,
        )

        SeekButtonIncrement.Unknown -> stringResource(id = if (skipForward) LR.string.skip_forward else LR.string.skip_back)
    }

    MediaButton(
        onClick = onClick,
        icon = icon,
        contentDescription = contentDescription,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        iconSize = iconSize,
        iconAlign = iconAlign,
    )
}
