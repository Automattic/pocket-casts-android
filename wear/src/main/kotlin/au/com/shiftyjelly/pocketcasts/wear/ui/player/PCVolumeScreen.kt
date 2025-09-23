package au.com.shiftyjelly.pocketcasts.wear.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.android.horologist.audio.ui.VolumeScreen
import com.google.android.horologist.compose.layout.ScreenScaffold

object PCVolumeScreen {
    const val ROUTE = "volume"
}

@Composable
fun PCVolumeScreen(
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        timeText = {},
        modifier = modifier,
    ) {
        VolumeScreen(
            volumeViewModel = hiltViewModel<PCVolumeViewModel>(),
        )
    }
}
