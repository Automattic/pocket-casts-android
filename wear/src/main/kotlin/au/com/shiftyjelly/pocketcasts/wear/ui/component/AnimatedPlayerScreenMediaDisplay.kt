package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import au.com.shiftyjelly.pocketcasts.R
import com.google.android.horologist.media.ui.components.InfoMediaDisplay
import com.google.android.horologist.media.ui.components.LoadingMediaDisplay
import com.google.android.horologist.media.ui.state.PlayerUiState
@Composable
fun AnimatedPlayerScreenMediaDisplay(
    playerUiState: PlayerUiState,
    modifier: Modifier = Modifier,
) {
    val media = playerUiState.media
    if (!playerUiState.connected) {
        LoadingMediaDisplay(modifier)
    } else if (media != null) {
        MarqueeTextMediaDisplay(
            modifier = modifier,
            title = media.title,
            artist = media.artist
        )
    } else {
        InfoMediaDisplay(
            message = stringResource(R.string.nothing_playing),
            modifier = modifier
        )
    }
}
