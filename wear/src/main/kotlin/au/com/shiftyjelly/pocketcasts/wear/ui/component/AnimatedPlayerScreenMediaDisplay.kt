package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Suppress("UnusedParameter")
@Composable
fun AnimatedPlayerScreenMediaDisplay(
    /*playerUiState: PlayerUiState,*/
    modifier: Modifier = Modifier,
) {
// TODO: Display UI based on player ui state media
//    val media = playerUiState.media
//    if (media != null) {
    MarqueeTextMediaDisplay(
        modifier = modifier,
        title = "A Really Long Podcast Name", // media.title,
        artist = "artist" // media.artist
    )
//    } else {
//        InfoMediaDisplay(
//            message = stringResource(R.string.horologist_nothing_playing),
//            modifier = modifier
//        )
//    }
}
