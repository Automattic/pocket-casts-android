package au.com.shiftyjelly.pocketcasts.wear.ui.player

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.wear.ui.component.ScreenHeaderChip
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState

object EffectsScreen {
    const val route = "effects"
}

@Composable
fun EffectsScreen(
    columnState: ScalingLazyColumnState,
    viewModel: EffectsViewModel = hiltViewModel(),
) {
    ScalingLazyColumn(
        columnState = columnState,
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            ScreenHeaderChip(text = R.string.effects)
        }
    }
}
