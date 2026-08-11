package au.com.shiftyjelly.pocketcasts.nowplaying

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import au.com.shiftyjelly.pocketcasts.component.LocalTvToastHostState
import au.com.shiftyjelly.pocketcasts.component.TvDropdownMenu
import au.com.shiftyjelly.pocketcasts.component.TvDropdownMenuItem
import au.com.shiftyjelly.pocketcasts.component.TvDropdownMenuSectionTitle
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.utils.extensions.roundedSpeed
import java.util.Locale
import kotlin.math.abs
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal val TvControlBarButtonSize = 64.dp
internal val TvControlBarIconSize = 24.dp
internal val TvControlBarButtonSpacing = 11.dp

internal val tvPlaybackSpeedOptions: List<Double> = (5..30).map { it / 10.0 }

internal fun playbackSpeedLabel(speed: Double, locale: Locale): String = String.format(locale, "%.1fx", speed)

internal fun nearestPlaybackSpeedOption(speed: Double): Double = tvPlaybackSpeedOptions.minBy { abs(it - speed) }

@Composable
internal fun TvPlaybackSpeedButton(
    speed: Double,
    isMenuVisible: Boolean,
    onMenuVisibleChange: (Boolean) -> Unit,
    onSelectSpeed: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentSpeed = speed.roundedSpeed()
    val locale = LocalResources.current.configuration.locales[0]
    Box(modifier = modifier) {
        IconButton(
            onClick = { onMenuVisibleChange(true) },
            colors = TvButtonDefaults.controlBarIconButtonColors(),
            modifier = Modifier.size(TvControlBarButtonSize),
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_effects_off),
                contentDescription = "${stringResource(LR.string.playback_speed)}, ${playbackSpeedLabel(currentSpeed, locale)}",
                modifier = Modifier.size(TvControlBarIconSize),
            )
        }
        if (isMenuVisible) {
            val toastHostState = LocalTvToastHostState.current
            val toastTemplate = stringResource(LR.string.tv_playback_speed_changed)
            val focusedOption = nearestPlaybackSpeedOption(currentSpeed)
            TvDropdownMenu(
                title = stringResource(LR.string.playback_speed),
                onDismissRequest = { onMenuVisibleChange(false) },
                maxHeight = 320.dp,
                alignment = Alignment.BottomCenter,
                offset = DpOffset(x = 0.dp, y = (-64).dp),
            ) {
                tvPlaybackSpeedOptions.forEach { option ->
                    TvDropdownMenuItem(
                        label = playbackSpeedLabel(option, locale),
                        isSelected = option == currentSpeed,
                        requestInitialFocus = option == focusedOption,
                        onClick = {
                            onMenuVisibleChange(false)
                            if (option != currentSpeed) {
                                onSelectSpeed(option)
                                toastHostState.show(
                                    String.format(locale, toastTemplate, playbackSpeedLabel(option, locale)),
                                )
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
    onSetVolumeBoost: (Boolean) -> Unit,
    onSelectTrimMode: (TrimMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        IconButton(
            onClick = { onMenuVisibleChange(true) },
            colors = TvButtonDefaults.controlBarIconButtonColors(),
            modifier = Modifier.size(TvControlBarButtonSize),
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_player_effects),
                contentDescription = stringResource(LR.string.player_effects),
                modifier = Modifier.size(TvControlBarIconSize),
            )
        }
        if (isMenuVisible) {
            val toastHostState = LocalTvToastHostState.current
            val locale = LocalResources.current.configuration.locales[0]
            val boostOnToast = stringResource(LR.string.tv_volume_boost_on)
            val boostOffToast = stringResource(LR.string.tv_volume_boost_off)
            val trimChangedTemplate = stringResource(LR.string.tv_trim_silence_changed)
            val trimOffToast = stringResource(LR.string.tv_trim_silence_off)
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
                        val isBoosted = !isVolumeBoosted
                        onSetVolumeBoost(isBoosted)
                        toastHostState.show(if (isBoosted) boostOnToast else boostOffToast)
                    },
                )
                TvDropdownMenuSectionTitle(text = stringResource(LR.string.player_effects_trim_silence))
                TrimMode.entries.forEach { mode ->
                    val label = stringResource(mode.labelId)
                    TvDropdownMenuItem(
                        label = label,
                        isSelected = mode == trimMode,
                        requestInitialFocus = false,
                        onClick = {
                            onMenuVisibleChange(false)
                            if (mode != trimMode) {
                                onSelectTrimMode(mode)
                                val toast = if (mode == TrimMode.OFF) {
                                    trimOffToast
                                } else {
                                    String.format(locale, trimChangedTemplate, label)
                                }
                                toastHostState.show(toast)
                            }
                        },
                    )
                }
            }
        }
    }
}
