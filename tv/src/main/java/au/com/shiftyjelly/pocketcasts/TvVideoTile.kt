package au.com.shiftyjelly.pocketcasts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import coil3.compose.AsyncImage

@Composable
fun TvVideoTile(
    thumbnailUrl: String,
    onPlayEpisode: () -> Unit,
    onGoToPodcast: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonState = rememberTvTileButtonState(buttonCount = 2)
    val buttonActions = remember(onPlayEpisode, onGoToPodcast) { listOf(onPlayEpisode, onGoToPodcast) }

    TvTile(
        onClick = onPlayEpisode,
        modifier = modifier.tvTileButtonNavigation(buttonState, buttonActions),
    ) {
        Box(
            modifier = Modifier
                .width(323.dp)
                .aspectRatio(16f / 9f),
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

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
                    .padding(14.dp),
                contentAlignment = Alignment.BottomStart,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.colors(
                            containerColor = if (buttonState.isButtonSelected(0)) Color.White else Color.White.copy(alpha = 0.7f),
                            contentColor = Color.Black,
                            focusedContainerColor = Color.White,
                            focusedContentColor = Color.Black,
                        ),
                    ) {
                        Text("Play this episode")
                    }
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.colors(
                            containerColor = if (buttonState.isButtonSelected(1)) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White,
                            focusedContainerColor = Color.White.copy(alpha = 0.4f),
                            focusedContentColor = Color.White,
                        ),
                    ) {
                        Text("Go to podcast")
                    }
                }
            }
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvVideoTilePreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            Box(modifier = Modifier.background(TvColors.Dark)) {
                TvVideoTile(
                    thumbnailUrl = "",
                    onPlayEpisode = {},
                    onGoToPodcast = {},
                )
            }
        }
    }
}
