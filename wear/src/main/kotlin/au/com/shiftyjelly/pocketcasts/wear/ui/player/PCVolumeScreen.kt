package au.com.shiftyjelly.pocketcasts.wear.ui.player

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.horologist.audio.ui.VolumeScreen
import com.google.android.horologist.compose.layout.ScreenScaffold

object PCVolumeScreen {
    const val route = "volume"
}

@Composable
fun PCVolumeScreen(
    volumeViewModel: PCVolumeViewModel = hiltViewModel(),
) {
    ScreenScaffold(
        timeText = {},
    ) {
        VolumeScreen(
            volumeViewModel = volumeViewModel,
        )
    }
}
