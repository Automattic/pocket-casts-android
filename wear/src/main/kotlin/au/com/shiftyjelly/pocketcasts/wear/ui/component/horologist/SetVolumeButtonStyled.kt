package au.com.shiftyjelly.pocketcasts.wear.ui.component.horologist

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.google.android.horologist.audio.ui.R
import com.google.android.horologist.audio.ui.VolumeUiState
import com.google.android.horologist.audio.ui.components.actions.SettingsButton
import com.google.android.horologist.compose.material.IconRtlMode

@Composable
fun SetVolumeButtonStyled(
    onVolumeClick: () -> Unit,
    modifier: Modifier = Modifier,
    volumeUiState: VolumeUiState? = null,
    enabled: Boolean = true,
    imageVolumeMute: ImageVector = Icons.Default.VolumeMute,
    imageVolume: ImageVector = Icons.Default.VolumeDown,
    imageVolumeMax: ImageVector = Icons.Default.VolumeUp,
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
        iconRtlMode = IconRtlMode.Mirrored,
        contentDescription = stringResource(R.string.horologist_set_volume_content_description)
    )
}
