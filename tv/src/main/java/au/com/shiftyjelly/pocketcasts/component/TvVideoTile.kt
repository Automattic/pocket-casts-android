package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import coil3.compose.AsyncImage
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvVideoTile(
    thumbnailUrl: String,
    podcastArtworkUrl: String,
    podcastTitle: String,
    episodeTitle: String,
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
                Crossfade(
                    targetState = buttonState.isFocused,
                    animationSpec = tween(durationMillis = 200),
                    label = "VideoTileBottomContent",
                ) { isFocused ->
                    if (isFocused) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Button(
                                onClick = {},
                                colors = tileButtonColors(isSelected = buttonState.isButtonSelected(0)),
                            ) {
                                Text(stringResource(LR.string.play_this_episode))
                            }
                            Button(
                                onClick = {},
                                colors = tileButtonColors(isSelected = buttonState.isButtonSelected(1)),
                            ) {
                                Text(stringResource(LR.string.go_to_podcast))
                            }
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model = podcastArtworkUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                            )
                            Column {
                                Text(
                                    text = podcastTitle,
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    ),
                                    color = TvColors.TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = episodeTitle,
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    ),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
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
private fun TvVideoTilePreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            Box(modifier = Modifier.background(TvColors.Dark)) {
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
}
