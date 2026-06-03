package au.com.shiftyjelly.pocketcasts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.OutlinedButtonDefaults
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import coil3.compose.AsyncImage

@Composable
fun TvFeaturedTile(
    artworkUrl: String,
    isSponsored: Boolean,
    title: String,
    description: String,
    onGoToPodcast: () -> Unit,
    onPlayLastEpisode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvTile(
        onClick = onPlayLastEpisode,
        scale = CardDefaults.scale(focusedScale = 1.05f),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.height(333.dp),
        ) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(333.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(24.dp),
            ) {
                if (isSponsored) {
                    Text(
                        text = "Sponsored",
                        style = TextStyle(
                            fontSize = 14.sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                        ),
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                    color = Color.White,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = description,
                    style = TextStyle(
                        fontSize = 16.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onPlayLastEpisode,
                        colors = OutlinedButtonDefaults.colors(
                            contentColor = Color.White,
                        ),
                    ) {
                        Text("Play latest episode")
                    }
                    OutlinedButton(
                        onClick = onGoToPodcast,
                        colors = OutlinedButtonDefaults.colors(
                            contentColor = Color.White,
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
private fun TvFeaturedTilePreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            Box(modifier = Modifier.background(TvColors.Dark)) {
                TvFeaturedTile(
                    artworkUrl = "",
                    isSponsored = true,
                    title = "Superhuman",
                    description = "SuperHuman is a high-stakes, edge-of-your-seat docuseries that dives into the launch of what many have called the \"Doping Olympics\"",
                    onGoToPodcast = {},
                    onPlayLastEpisode = {},
                )
            }
        }
    }
}
