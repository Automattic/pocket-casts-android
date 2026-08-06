package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.theme.TvTextStyles
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvPlaylistCard(
    title: String,
    isSmartPlaylist: Boolean,
    episodeCount: Int?,
    artworkUrls: List<String>,
    cardColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val backgroundColor by animateColorAsState(
        targetValue = if (isFocused) TvColors.BackgroundActive else cardColor,
        animationSpec = tween(durationMillis = 200),
        label = "PlaylistCardBackground",
    )
    val titleColor by animateColorAsState(
        targetValue = if (isFocused) TvColors.TextPrimaryActive else TvColors.TextPrimary,
        animationSpec = tween(durationMillis = 200),
        label = "PlaylistCardTitleColor",
    )
    val captionColor by animateColorAsState(
        targetValue = if (isFocused) TvColors.TextSecondaryActive else TvColors.TextSecondary,
        animationSpec = tween(durationMillis = 200),
        label = "PlaylistCardCaptionColor",
    )
    val highlightAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "PlaylistCardHighlight",
    )

    TvTile(
        onClick = onClick,
        scale = CardDefaults.scale(focusedScale = 1.05f),
        shape = CardDefaults.shape(RoundedCornerShape(11.dp)),
        colors = CardDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
        ),
        glow = CardDefaults.glow(
            focusedGlow = Glow(elevationColor = Color.Black, elevation = 16.dp),
        ),
        interactionSource = interactionSource,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(568f / 258f)
                .background(backgroundColor),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Text(
                    text = title,
                    style = TvTextStyles.Callout,
                    color = titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isSmartPlaylist) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(LR.string.smart_playlist),
                        style = TvTextStyles.Caption2,
                        color = captionColor,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (episodeCount != null) {
                    Text(
                        text = pluralStringResource(LR.plurals.episode_count, episodeCount, episodeCount),
                        style = TvTextStyles.Caption2,
                        color = captionColor,
                    )
                }
            }

            PlaylistCovers(
                artworkUrls = artworkUrls,
                isFocused = isFocused,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(highlightAlpha)
                    .background(
                        Brush.linearGradient(
                            colorStops = arrayOf(
                                0f to Color.White.copy(alpha = 0.16f),
                                0.2f to Color.White.copy(alpha = 0.06f),
                                0.4f to Color.White.copy(alpha = 0.02f),
                                0.6f to Color.Transparent,
                            ),
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun PlaylistCovers(
    artworkUrls: List<String>,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        artworkUrls.take(2).reversed().forEachIndexed { index, artworkUrl ->
            PlaylistCover(
                artworkUrl = artworkUrl,
                isFocused = isFocused,
                isBackCover = index == 0,
            )
        }
    }
}

@Composable
private fun PlaylistCover(
    artworkUrl: String,
    isFocused: Boolean,
    isBackCover: Boolean,
) {
    val animationSpec = spring<Float>(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)
    val rotation by animateFloatAsState(
        targetValue = when {
            !isFocused -> 0f
            isBackCover -> 15f
            else -> -15f
        },
        animationSpec = animationSpec,
        label = "PlaylistCoverRotation",
    )
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1f,
        animationSpec = animationSpec,
        label = "PlaylistCoverScale",
    )
    val restingOffset by animateFloatAsState(
        targetValue = if (isBackCover && !isFocused) 5.3f else 0f,
        animationSpec = animationSpec,
        label = "PlaylistCoverOffset",
    )

    val offsetX = (if (isBackCover) 0.7f else -16f) + restingOffset
    val offsetY = (if (isBackCover) 66.7f else 44.7f) + restingOffset

    TvArtworkImage(
        model = artworkUrl,
        modifier = Modifier
            .offset(x = offsetX.dp, y = offsetY.dp)
            .size(104.dp)
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp)),
    )
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvPlaylistCardPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            Box(modifier = Modifier.background(TvColors.BackgroundSunken).padding(32.dp)) {
                TvPlaylistCard(
                    title = "New Releases",
                    isSmartPlaylist = true,
                    episodeCount = 24,
                    artworkUrls = listOf("", ""),
                    cardColor = TvPlaylistCardColors.cardColor(podcastTint = null, seed = "new-releases"),
                    onClick = {},
                )
            }
        }
    }
}
