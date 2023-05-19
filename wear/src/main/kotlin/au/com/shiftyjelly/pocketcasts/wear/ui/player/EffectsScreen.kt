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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.wear.ui.component.ScreenHeaderChip
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
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
    ScalingLazyColumn(
        columnState = columnState,
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            ScreenHeaderChip(text = R.string.effects)
        }
        when (state) {
            EffectsViewModel.State.Loading -> Unit
            is EffectsViewModel.State.Loaded -> {
                item {
                    SpeedChip(
                        state = state,
                        onPlusClicked = { viewModel.increasePlaybackSpeed() },
                        onMinusClicked = { viewModel.decreasePlaybackSpeed() },
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
