package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.horologist.composables.MarqueeText
import com.google.android.horologist.media.ui.ExperimentalHorologistMediaUiApi

/**
 * An animated text only display showing scrolling title and still artist in two separated rows.
 */
@ExperimentalHorologistMediaUiApi
@Composable
fun MarqueeTextMediaDisplay(
    modifier: Modifier = Modifier,
    title: String? = null,
    artist: String? = null
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        MarqueeText(
            text = title.orEmpty(),
            modifier = Modifier.fillMaxWidth(0.7f),
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.button,
            textAlign = TextAlign.Center
        )
        Text(
            text = artist.orEmpty(),
            modifier = Modifier.fillMaxWidth(0.8f),
            color = MaterialTheme.colors.onBackground,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.body2
        )
    }
}
