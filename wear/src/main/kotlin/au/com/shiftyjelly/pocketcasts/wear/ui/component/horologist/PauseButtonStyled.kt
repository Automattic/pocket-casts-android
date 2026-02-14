package au.com.shiftyjelly.pocketcasts.wear.ui.component.horologist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ButtonColors
import androidx.wear.compose.material.ButtonDefaults
import com.google.android.horologist.media.ui.components.controls.MediaButton
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun PauseButtonStyled(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.iconButtonColors(),
    iconSize: Dp = 30.dp,
    icon: ImageVector = ImageVector.vectorResource(IR.drawable.wear_pause),
) {
    MediaButton(
        onClick = onClick,
        icon = icon,
        contentDescription = stringResource(id = LR.string.pause),
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        iconSize = iconSize,
    )
}
