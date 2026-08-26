package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import coil3.compose.AsyncImage
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvPodcastTile(
    artworkUrl: String,
    podcastTitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    isSponsored: Boolean = false,
) {
    TvTile(
        onClick = onClick,
        modifier = modifier,
    ) {
        if (isSponsored) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = podcastTitle,
                    contentScale = ContentScale.Crop,
                    modifier = imageModifier
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                        .aspectRatio(1f),
                )
                Text(
                    text = stringResource(LR.string.sponsored),
                    style = MaterialTheme.tvTypography.caption2,
                    color = MaterialTheme.tvColors.textSecondary,
                    modifier = Modifier.padding(bottom = 9.dp),
                )
            }
        } else {
            AsyncImage(
                model = artworkUrl,
                contentDescription = podcastTitle,
                contentScale = ContentScale.Crop,
                modifier = imageModifier.aspectRatio(1f),
            )
        }
    }
}

object TvPodcastTileDefaults {
    val RowImageWidth = 123.dp
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvPodcastTilePreview() {
    TvTheme {
        Box(modifier = Modifier.background(MaterialTheme.tvColors.backgroundSunken)) {
            TvPodcastTile(
                artworkUrl = "",
                podcastTitle = "Sample Podcast",
                onClick = {},
                imageModifier = Modifier.width(TvPodcastTileDefaults.RowImageWidth),
            )
        }
    }
}
