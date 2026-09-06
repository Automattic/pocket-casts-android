package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import coil3.compose.AsyncImage
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvFeaturedTile(
    artworkUrl: String,
    isSponsored: Boolean,
    title: String,
    author: String,
    description: String,
    onGoToPodcast: () -> Unit,
    onPlayLastEpisode: () -> Unit,
    modifier: Modifier = Modifier,
    sponsoredLabel: String? = null,
) {
    val buttonState = rememberTvTileButtonState(buttonCount = 2)
    val buttonActions = remember(onPlayLastEpisode, onGoToPodcast) { listOf(onPlayLastEpisode, onGoToPodcast) }

    TvTile(
        onClick = onPlayLastEpisode,
        scale = CardDefaults.scale(focusedScale = 1.05f),
        colors = CardDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
        ),
        glow = CardDefaults.glow(
            focusedGlow = Glow(elevationColor = Color.Black, elevation = 16.dp),
        ),
        modifier = modifier.tvTileButtonNavigation(buttonState, buttonActions),
    ) {
        Box(
            modifier = Modifier
                .width(802.dp)
                .height(250.dp),
        ) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(30.dp),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.2f to MaterialTheme.tvColors.backgroundSunken.copy(alpha = 0.7f),
                                0.45f to MaterialTheme.tvColors.backgroundSunken,
                                1f to MaterialTheme.tvColors.backgroundSunken,
                            ),
                        ),
                    ),
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(20.dp)
                        .size(210.dp)
                        .clip(RoundedCornerShape(3.dp)),
                )

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(horizontal = 10.5.dp, vertical = 13.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (isSponsored) {
                        Text(
                            text = sponsoredLabel ?: stringResource(LR.string.sponsored),
                            style = MaterialTheme.tvTypography.caption2,
                            color = MaterialTheme.tvColors.textPrimary70,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (author.isNotBlank()) {
                        Text(
                            text = author,
                            style = MaterialTheme.tvTypography.caption2,
                            color = MaterialTheme.tvColors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.tvTypography.title2,
                        color = MaterialTheme.tvColors.textPrimary,
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = description,
                        style = MaterialTheme.tvTypography.caption2,
                        color = MaterialTheme.tvColors.textPrimary70,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = onPlayLastEpisode,
                            colors = tileButtonColors(isSelected = buttonState.isButtonSelected(0)),
                        ) {
                            Text(stringResource(LR.string.play_latest_episode))
                        }
                        Button(
                            onClick = onGoToPodcast,
                            colors = tileButtonColors(isSelected = buttonState.isButtonSelected(1)),
                        ) {
                            Text(stringResource(LR.string.go_to_podcast))
                        }
                    }
                }
            }
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvFeaturedTilePreview() {
    TvTheme {
        Box(modifier = Modifier.background(MaterialTheme.tvColors.backgroundSunken)) {
            TvFeaturedTile(
                artworkUrl = "",
                isSponsored = true,
                sponsoredLabel = "Sponsored \u00B7 iHeartPodcasts and Kaleidoscope",
                title = "Superhuman",
                author = "iHeartPodcasts",
                description = "SuperHuman is a high-stakes, edge-of-your-seat docuseries that dives into the launch of what many have called the \"Doping Olympics\"",
                onGoToPodcast = {},
                onPlayLastEpisode = {},
            )
        }
    }
}
