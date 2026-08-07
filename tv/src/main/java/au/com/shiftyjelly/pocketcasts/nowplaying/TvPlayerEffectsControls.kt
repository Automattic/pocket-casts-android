package au.com.shiftyjelly.pocketcasts.nowplaying

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.component.TvDropdownMenu
import au.com.shiftyjelly.pocketcasts.component.TvDropdownMenuItem
import au.com.shiftyjelly.pocketcasts.component.TvDropdownMenuSectionTitle
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import au.com.shiftyjelly.pocketcasts.utils.extensions.roundedSpeed
import java.util.Locale
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal val tvPlaybackSpeedOptions: List<Double> = (5..30).map { it / 10.0 }

internal fun playbackSpeedLabel(speed: Double): String = String.format(Locale.getDefault(), "%.1fx", speed)

@Composable
internal fun TvPlaybackSpeedButton(
    speed: Double,
    isMenuVisible: Boolean,
    onMenuVisibleChange: (Boolean) -> Unit,
    onSelectSpeed: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentSpeed = speed.roundedSpeed()
    Box(modifier = modifier) {
        IconButton(
            onClick = { onMenuVisibleChange(true) },
            colors = TvButtonDefaults.iconButtonColors(),
            modifier = Modifier.size(56.dp),
        ) {
            Text(
                text = playbackSpeedLabel(currentSpeed),
                style = MaterialTheme.tvTypography.caption1,
            )
        }
        if (isMenuVisible) {
            TvDropdownMenu(
                title = stringResource(LR.string.playback_speed),
                onDismissRequest = { onMenuVisibleChange(false) },
                maxHeight = 320.dp,
                alignment = Alignment.BottomCenter,
                offset = DpOffset(x = 0.dp, y = (-64).dp),
            ) {
                tvPlaybackSpeedOptions.forEach { option ->
                    TvDropdownMenuItem(
                        label = playbackSpeedLabel(option),
                        isSelected = option == currentSpeed,
                        onClick = {
                            onMenuVisibleChange(false)
                            if (option != currentSpeed) {
                                onSelectSpeed(option)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun TvPlayerEffectsButton(
    trimMode: TrimMode,
    isVolumeBoosted: Boolean,
    isMenuVisible: Boolean,
    onMenuVisibleChange: (Boolean) -> Unit,
    onToggleVolumeBoost: () -> Unit,
    onSelectTrimMode: (TrimMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        IconButton(
            onClick = { onMenuVisibleChange(true) },
            colors = TvButtonDefaults.iconButtonColors(),
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_effects_off),
                contentDescription = stringResource(LR.string.player_effects),
                modifier = Modifier.size(24.dp),
            )
        }
        if (isMenuVisible) {
            TvDropdownMenu(
                title = stringResource(LR.string.player_effects),
                onDismissRequest = { onMenuVisibleChange(false) },
                alignment = Alignment.BottomCenter,
                offset = DpOffset(x = 0.dp, y = (-64).dp),
            ) {
                TvDropdownMenuItem(
                    label = stringResource(LR.string.player_effects_volume_boost),
                    isSelected = isVolumeBoosted,
                    requestInitialFocus = true,
                    onClick = {
                        onMenuVisibleChange(false)
                        onToggleVolumeBoost()
                    },
                )
                TvDropdownMenuSectionTitle(text = stringResource(LR.string.player_effects_trim_silence))
                TrimMode.entries.forEach { mode ->
                    TvDropdownMenuItem(
                        label = stringResource(mode.labelId),
                        isSelected = mode == trimMode,
                        requestInitialFocus = false,
                        onClick = {
                            onMenuVisibleChange(false)
                            if (mode != trimMode) {
                                onSelectTrimMode(mode)
                            }
                        },
                    )
                }
            }
        }
    }
}
