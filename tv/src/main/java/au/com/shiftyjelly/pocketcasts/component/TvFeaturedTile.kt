package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import coil3.compose.AsyncImage
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvFeaturedTile(
    artworkUrl: String,
    isSponsored: Boolean,
    title: String,
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
        modifier = modifier.tvTileButtonNavigation(buttonState, buttonActions),
    ) {
        Box(
            modifier = Modifier
                .width(642.dp)
                .height(200.dp),
        ) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(40.dp),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.2f to TvColors.Dark.copy(alpha = 0.7f),
                                0.45f to TvColors.Dark,
                                1f to TvColors.Dark,
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
                        .padding(16.dp)
                        .size(168.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 17.dp),
                ) {
                    if (isSponsored) {
                        Text(
                            text = sponsoredLabel ?: "Sponsored",
                            style = TextStyle(
                                fontSize = 14.sp,
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                            ),
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = title,
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                        ),
                        color = Color.White,
                    )

                    Spacer(modifier = Modifier.height(7.dp))

                    Text(
                        text = description,
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                        ),
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    AnimatedVisibility(
                        visible = buttonState.isFocused,
                        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedButton(
                                onClick = {},
                                colors = tileButtonColors(isSelected = buttonState.isButtonSelected(0)),
                            ) {
                                Text(stringResource(LR.string.play_latest_episode))
                            }
                            OutlinedButton(
                                onClick = {},
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
                    sponsoredLabel = "Sponsored \u00B7 iHeartPodcasts and Kaleidoscope",
                    title = "Superhuman",
                    description = "SuperHuman is a high-stakes, edge-of-your-seat docuseries that dives into the launch of what many have called the \"Doping Olympics\"",
                    onGoToPodcast = {},
                    onPlayLastEpisode = {},
                )
            }
        }
    }
}
