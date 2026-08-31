package au.com.shiftyjelly.pocketcasts.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import coil3.compose.AsyncImage

@Composable
fun TvVideoTile(
    thumbnailUrl: String,
    podcastArtworkUrl: String,
    podcastTitle: String,
    episodeTitle: String,
    onPlayEpisode: () -> Unit,
    onGoToPodcast: () -> Unit,
    modifier: Modifier = Modifier,
    videoPreviewUrl: String? = null,
    isPodcastPlaying: () -> Boolean = { false },
) {
    var isFocused by remember { mutableStateOf(false) }

    TvTile(
        onClick = onPlayEpisode,
        onLongClick = onGoToPodcast,
        modifier = modifier.onFocusChanged { isFocused = it.hasFocus },
    ) {
        Box(
            modifier = Modifier
                .width(242.dp)
                .aspectRatio(16f / 9f),
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            if (videoPreviewUrl != null) {
                var posterFrame by remember(videoPreviewUrl) { mutableStateOf<Bitmap?>(null) }
                LaunchedEffect(videoPreviewUrl) {
                    posterFrame = TvVideoPreviewFrameLoader.frameFor(videoPreviewUrl)
                }
                posterFrame?.let { frame ->
                    Image(
                        bitmap = frame.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                TvVideoPreviewPlayer(
                    videoUrl = videoPreviewUrl,
                    isFocused = isFocused,
                    isPodcastPlaying = isPodcastPlaying,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                        ),
                    )
                    .padding(10.5.dp),
                contentAlignment = Alignment.BottomStart,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = podcastArtworkUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(27.dp)
                            .clip(RoundedCornerShape(3.dp)),
                    )
                    Column {
                        Text(
                            text = podcastTitle,
                            style = MaterialTheme.tvTypography.caption1,
                            color = MaterialTheme.tvColors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = episodeTitle,
                            style = MaterialTheme.tvTypography.caption1,
                            color = MaterialTheme.tvColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvVideoTilePreview() {
    TvTheme {
        Box(modifier = Modifier.background(MaterialTheme.tvColors.backgroundSunken)) {
            TvVideoTile(
                thumbnailUrl = "",
                podcastArtworkUrl = "",
                podcastTitle = "Huberman Lab",
                episodeTitle = "How to overcome Social Anxiety",
                onPlayEpisode = {},
                onGoToPodcast = {},
            )
        }
    }
}
