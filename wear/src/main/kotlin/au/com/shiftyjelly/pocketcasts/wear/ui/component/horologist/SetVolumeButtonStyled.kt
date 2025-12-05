package au.com.shiftyjelly.pocketcasts.wear.ui.component.horologist

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.google.android.horologist.audio.ui.VolumeUiState
import com.google.android.horologist.audio.ui.components.actions.SettingsButton
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun SetVolumeButtonStyled(
    onVolumeClick: () -> Unit,
    modifier: Modifier = Modifier,
    volumeUiState: VolumeUiState? = null,
    enabled: Boolean = true,
    imageVolumeMute: ImageVector = Icons.AutoMirrored.Filled.VolumeMute,
    imageVolume: ImageVector = Icons.AutoMirrored.Filled.VolumeDown,
    imageVolumeMax: ImageVector = Icons.AutoMirrored.Filled.VolumeUp,
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
