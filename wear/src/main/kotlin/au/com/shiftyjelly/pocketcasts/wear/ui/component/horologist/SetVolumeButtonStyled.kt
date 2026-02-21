package au.com.shiftyjelly.pocketcasts.wear.ui.component.horologist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.google.android.horologist.audio.ui.VolumeUiState
import com.google.android.horologist.audio.ui.components.actions.SettingsButton
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun SetVolumeButtonStyled(
    onVolumeClick: () -> Unit,
    modifier: Modifier = Modifier,
    volumeUiState: VolumeUiState? = null,
    enabled: Boolean = true,
    imageVolumeMute: ImageVector = ImageVector.vectorResource(IR.drawable.wear_volume_mute),
    imageVolume: ImageVector = ImageVector.vectorResource(IR.drawable.wear_volume),
    imageVolumeMax: ImageVector = ImageVector.vectorResource(IR.drawable.wear_volume_max),
) {
    SettingsButton(
        modifier = modifier,
        onClick = onVolumeClick,
        enabled = enabled,
        imageVector = when {
            volumeUiState?.isMin == true -> imageVolumeMute
            volumeUiState?.isMax == false -> imageVolume
            else -> imageVolumeMax // volumeUiState == null || volumeUiState.isMax == true
        },
        contentDescription = stringResource(LR.string.set_volume),
    )
}
