package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.horologist.composables.MarqueeText
import com.google.android.horologist.media.ui.ExperimentalHorologistMediaUiApi
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

/**
 * An animated text only display showing scrolling title and still artist in two separated rows.
 */
@ExperimentalHorologistMediaUiApi
@Composable
fun MarqueeTextMediaDisplay(
    modifier: Modifier = Modifier,
    title: String? = null,
    artist: String? = null,
    isPlaybackError: Boolean = false,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        MarqueeText(
            text = title.orEmpty(),
            modifier = Modifier.fillMaxWidth(0.7f),
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.button,
            textAlign = TextAlign.Center
        )
        Row(modifier = Modifier.fillMaxWidth(0.8f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (isPlaybackError) {
                Image(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(id = IR.drawable.playback_error),
                    contentDescription = stringResource(id = LR.string.podcast_episode_playback_error),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = artist.orEmpty(),
                color = MaterialTheme.colors.onBackground,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = MaterialTheme.typography.caption3
            )
        }
    }
}

@Preview
@Composable
fun MarqueeTextMediaDisplayPreview() {
    MarqueeTextMediaDisplay(
        title = "Title",
        artist = "Artist",
        isPlaybackError = true
    )
}
