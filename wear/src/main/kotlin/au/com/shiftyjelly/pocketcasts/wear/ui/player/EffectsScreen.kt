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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.InlineSlider
import androidx.wear.compose.material.InlineSliderDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.tooling.preview.devices.WearDevices
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffectsData
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.wear.ui.component.ScreenHeaderChip
import au.com.shiftyjelly.pocketcasts.wear.ui.settings.ToggleChip
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberColumnState
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object EffectsScreen {
    const val ROUTE = "effects"
}

@Composable
fun EffectsScreen(
    viewModel: EffectsViewModel = hiltViewModel(),
) {
    val columnState = rememberColumnState()

    ScreenScaffold(
        scrollState = columnState,
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
}

@Composable
private fun Content(
    columnState: ScalingLazyColumnState,
    state: EffectsViewModel.State,
    increasePlaybackSpeed: () -> Unit,
    decreasePlaybackSpeed: () -> Unit,
    updateTrimSilence: (TrimMode) -> Unit,
    updateBoostVolume: (Boolean) -> Unit,
) {
    ScalingLazyColumn(
        columnState = columnState,
        modifier = Modifier.fillMaxWidth(),
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
                        onPlusClick = { increasePlaybackSpeed() },
                        onMinusClick = { decreasePlaybackSpeed() },
                    )
                }
                val trimSilence = state.playbackEffects.trimMode != TrimMode.OFF
                item {
                    ToggleChip(
                        label = stringResource(LR.string.player_effects_trim_silence),
                        checked = trimSilence,
                        onToggle = { value ->
                            val newValue = if (value) TrimMode.LOW else TrimMode.OFF
                            updateTrimSilence(newValue)
                        },
                    )
                }
                if (trimSilence) {
                    item {
                        TrimSilenceSlider(
                            trimMode = state.playbackEffects.trimMode,
                            onValueChange = {
                                updateTrimSilence(TrimMode.values()[it])
                            },
                        )
                    }
                }
                item {
                    ToggleChip(
                        label = stringResource(LR.string.player_effects_volume_boost),
                        checked = state.playbackEffects.isVolumeBoosted,
                        onToggle = { updateBoostVolume(it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedChip(
    state: EffectsViewModel.State.Loaded,
    onPlusClick: () -> Unit,
    onMinusClick: () -> Unit,
) {
    Chip(
        onClick = { },
        colors = ChipDefaults.secondaryChipColors(),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
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
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(id = LR.string.player_effects_speed),
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.button.copy(
                            color = Color.White,
                        ),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    IconButton(onClick = onMinusClick) {
                        Icon(
                            painter = painterResource(IR.drawable.minus_simple),
                            contentDescription = stringResource(LR.string.player_effects_speed_up),
                        )
                    }
                    TextH30(
                        text = String.format("%.1fx", state.playbackEffects.playbackSpeed),
                        color = MaterialTheme.colors.onPrimary,
                    )
                    IconButton(onClick = onPlusClick) {
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
private fun TrimSilenceSlider(
    trimMode: TrimMode,
    onValueChange: (Int) -> Unit,
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
                    .size(20.dp),
            )
            TextH50(
                text = stringResource(id = LR.string.player_effects_trim_silence),
                fontWeight = FontWeight.W700,
                color = MaterialTheme.colors.onPrimary,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        InlineSlider(
            value = trimMode.ordinal.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            increaseIcon = {
                Icon(
                    InlineSliderDefaults.Increase,
                    stringResource(LR.string.player_effects_trim_level_up),
                )
            },
            decreaseIcon = {
                Icon(
                    InlineSliderDefaults.Decrease,
                    stringResource(LR.string.player_effects_trim_level_down),
                )
            },
            valueRange = 0f..3f,
            steps = 2,
            segmented = true,
            colors = InlineSliderDefaults.colors(
                selectedBarColor = MaterialTheme.colors.onSurface,
            ),
        )
    }
}

@Preview(
    device = WearDevices.LARGE_ROUND,
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true,
)
@Composable
private fun EffectsScreenDarkPreview() {
    AppTheme(themeType = Theme.ThemeType.DARK) {
        Content(
            columnState = rememberResponsiveColumnState(),
            state = EffectsViewModel.State.Loaded(
                playbackEffects = PlaybackEffectsData(
                    trimMode = TrimMode.MEDIUM,
                    playbackSpeed = 1.5,
                    isVolumeBoosted = true,
                ),
            ),
            increasePlaybackSpeed = {},
            decreasePlaybackSpeed = {},
            updateTrimSilence = {},
            updateBoostVolume = {},
        )
    }
}
