package au.com.shiftyjelly.pocketcasts.wear.ui.player

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.horologist.audio.ui.VolumeScreen

object PCVolumeScreen {
    const val route = "volume"
}

@Composable
fun PCVolumeScreen(
    volumeViewModel: PCVolumeViewModel = hiltViewModel()
) {
    VolumeScreen(
        volumeViewModel = volumeViewModel
    )
}
