package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import coil3.compose.AsyncImage

@Composable
fun TvPodcastTile(
    artworkUrl: String,
    podcastTitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvTile(
        onClick = onClick,
        modifier = modifier,
    ) {
        AsyncImage(
            model = artworkUrl,
            contentDescription = podcastTitle,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(123.dp)
                .aspectRatio(1f),
        )
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvPodcastTilePreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            Box(modifier = Modifier.background(TvColors.Dark)) {
                TvPodcastTile(
                    artworkUrl = "",
                    podcastTitle = "Sample Podcast",
                    onClick = {},
                )
            }
        }
    }
}
