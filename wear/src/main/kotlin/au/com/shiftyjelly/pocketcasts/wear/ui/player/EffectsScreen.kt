package au.com.shiftyjelly.pocketcasts.wear.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.InlineSlider
import androidx.wear.compose.material.InlineSliderDefaults
import androidx.wear.compose.material.MaterialTheme
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.wear.theme.theme
import au.com.shiftyjelly.pocketcasts.wear.ui.ToggleChip
import au.com.shiftyjelly.pocketcasts.wear.ui.component.ScreenHeaderChip
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import com.google.android.horologist.compose.layout.belowTimeTextPreview
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object EffectsScreen {
    const val route = "effects"
}

@Composable
fun EffectsScreen(
    columnState: ScalingLazyColumnState,
    viewModel: EffectsViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    Content(
        columnState = columnState,
        state = state,
        increasePlaybackSpeed = { viewModel.increasePlaybackSpeed() },
        decreasePlaybackSpeed = { viewModel.decreasePlaybackSpeed() },
        updateTrimSilence = { viewModel.updateTrimSilence(it) },
        updateBoostVolume = { viewModel.updateBoostVolume(it) },
    )
}

@Composable
fun Content(
    columnState: ScalingLazyColumnState,
    state: EffectsViewModel.State,
    increasePlaybackSpeed: () -> Unit,
    decreasePlaybackSpeed: () -> Unit,
    updateTrimSilence: (TrimMode) -> Unit,
    updateBoostVolume: (Boolean) -> Unit,
) {
    ScalingLazyColumn(
        columnState = columnState,
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            ScreenHeaderChip(text = LR.string.effects)
        }
        when (state) {
            EffectsViewModel.State.Loading -> Unit
            is EffectsViewModel.State.Loaded -> {
                item {
                    SpeedChip(
                        state = state,
                        onPlusClicked = { increasePlaybackSpeed() },
                        onMinusClicked = { decreasePlaybackSpeed() },
                    )
                }
                val trimSilence = state.playbackEffects.trimMode != TrimMode.OFF
                item {
                    ToggleChip(
                        label = stringResource(LR.string.player_effects_trim_silence),
                        checked = trimSilence,
                        onCheckedChanged = { value ->
                            val newValue = if (value) TrimMode.LOW else TrimMode.OFF
                            updateTrimSilence(newValue)
                        },
                    )
                }
                if (trimSilence) {
                    item {
                        TrimSilenceSlider(
                            trimMode = state.playbackEffects.trimMode,
                            onValueChanged = {
                                updateTrimSilence(TrimMode.values()[it])
                            },
                        )
                    }
                }
                item {
                    ToggleChip(
                        label = stringResource(LR.string.player_effects_volume_boost),
                        checked = state.playbackEffects.isVolumeBoosted,
                        onCheckedChanged = { updateBoostVolume(it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedChip(
    state: EffectsViewModel.State.Loaded,
    modifier: Modifier = Modifier,
    onPlusClicked: () -> Unit,
    onMinusClicked: () -> Unit,
) {
    Chip(
        onClick = { },
        colors = ChipDefaults.secondaryChipColors(),
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .height(96.dp)
            .fillMaxWidth(),
        label = {
            Column(
                verticalArrangement = Arrangement.Center,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                ) {
                    Icon(
                        painter = painterResource(IR.drawable.ic_speed),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(id = LR.string.player_effects_speed),
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.button.copy(
                            color = Color.White
                        ),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = onMinusClicked) {
                        Icon(
                            painter = painterResource(IR.drawable.minus_simple),
                            contentDescription = stringResource(LR.string.player_effects_speed_up),
                        )
                    }
                    TextH30(text = String.format("%.1fx", state.playbackEffects.playbackSpeed))
                    IconButton(onClick = onPlusClicked) {
                        Icon(
                            painter = painterResource(IR.drawable.plus_simple),
                            contentDescription = stringResource(LR.string.player_effects_speed_down),
                        )
                    }
                }
            }
        },
    )
}

@Composable
fun TrimSilenceSlider(
    trimMode: TrimMode,
    onValueChanged: (Int) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_trim),
                contentDescription = stringResource(id = LR.string.player_effects_trim_silence),
                modifier = Modifier
                    .padding(start = 24.dp)
                    .size(20.dp)
            )
            TextH50(
                text = stringResource(id = LR.string.player_effects_trim_silence),
                fontWeight = FontWeight.W700,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        InlineSlider(
            value = trimMode.ordinal.toFloat(),
            onValueChange = { onValueChanged(it.toInt()) },
            increaseIcon = {
                Icon(
                    InlineSliderDefaults.Increase,
                    stringResource(LR.string.player_effects_trim_level_up)
                )
            },
            decreaseIcon = {
                Icon(
                    InlineSliderDefaults.Decrease,
                    stringResource(LR.string.player_effects_trim_level_down)
                )
            },
            valueRange = 0f..3f,
            steps = 2,
            segmented = true,
            colors = InlineSliderDefaults.colors(
                selectedBarColor = MaterialTheme.theme.colors.support05
            )
        )
    }
}

@Preview(
    device = Devices.WEAR_OS_LARGE_ROUND,
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true
)
@Composable
fun EffectsScreenDarkPreview() {
    AppTheme(themeType = Theme.ThemeType.DARK) {
        Content(
            columnState = belowTimeTextPreview(),
            state = EffectsViewModel.State.Loaded(
                playbackEffects = PlaybackEffects().apply {
                    trimMode = TrimMode.MEDIUM
                    playbackSpeed = 1.5
                    isVolumeBoosted = true
                }
            ),
            increasePlaybackSpeed = {},
            decreasePlaybackSpeed = {},
            updateTrimSilence = {},
            updateBoostVolume = {},
        )
    }
}
