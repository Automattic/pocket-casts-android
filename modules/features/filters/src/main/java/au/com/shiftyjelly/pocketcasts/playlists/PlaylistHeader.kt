package au.com.shiftyjelly.pocketcasts.playlists

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.PreviewRegularDevice
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType

internal data class PlaylistHeaderData(
    val title: String,
    val artworkPodcasts: List<Podcast>,
)

@Composable
internal fun PlaylistHeader(
    data: PlaylistHeaderData?,
    useBlurredArtwork: Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
    ) {
        val podcasts = data?.artworkPodcasts

        val toolbarInsets = AppBarDefaults.topAppBarWindowInsets
        val toolbarTopPadding = toolbarInsets.asPaddingValues().calculateTopPadding()
        val toolbarHeight = 48.dp
        val artworkTopPadding = 16.dp
        val artworkSize = minOf(maxWidth * 0.48f, 192.dp)

        PlaylistBackgroundArtwork(
            podcasts = podcasts,
            useBlurredArtwork = useBlurredArtwork,
            maxWidth = maxWidth,
            bottomAnchor = toolbarTopPadding + toolbarHeight + artworkTopPadding + artworkSize * 0.75f,
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Spacer(
                modifier = Modifier.height(toolbarTopPadding + toolbarHeight + artworkTopPadding),
            )
            Crossfade(
                targetState = podcasts,
                animationSpec = artworkCrossfadeSpec,
            ) { podcasts ->
                if (podcasts != null) {
                    PlaylistArtwork(
                        podcasts = podcasts,
                        artworkSize = artworkSize,
                    )
                } else {
                    Spacer(
                        modifier = Modifier.height(artworkSize),
                    )
                }
            }
            // Temporary empty space
            Spacer(
                modifier = Modifier.height(100.dp),
            )
        }
    }
}

@Composable
private fun PlaylistBackgroundArtwork(
    podcasts: List<Podcast>?,
    useBlurredArtwork: Boolean,
    maxWidth: Dp,
    bottomAnchor: Dp,
    modifier: Modifier = Modifier,
) {
    val artworkSize = maxWidth * if (useBlurredArtwork) 1.3f else 1f
    val artworkSizePx = LocalDensity.current.run { artworkSize.roundToPx() }

    val artworkBottomOffset = artworkSize - bottomAnchor
    val artworkBottomOffsetPx = LocalDensity.current.run { artworkBottomOffset.roundToPx() }

    Crossfade(
        targetState = podcasts?.takeIf { it.isNotEmpty() },
        animationSpec = artworkCrossfadeSpec,
        modifier = modifier
            .layout { measurable, constraints ->
                val artworkHeightPx = if (useBlurredArtwork) {
                    artworkSizePx
                } else {
                    artworkSizePx - artworkBottomOffsetPx
                }
                val const = constraints.copy(
                    minWidth = artworkSizePx,
                    maxWidth = artworkSizePx,
                    minHeight = artworkHeightPx,
                    maxHeight = artworkHeightPx,
                )

                val placeable = measurable.measure(const)
                val width = const.constrainWidth(placeable.width)
                val height = const.constrainHeight(placeable.height)
                val offset = if (useBlurredArtwork) {
                    artworkBottomOffsetPx
                } else {
                    0
                }
                layout(width, height - offset) { placeable.place(0, -offset) }
            }
            .blurOrScrim(useBlur = useBlurredArtwork),
    ) { podcasts ->
        if (podcasts != null) {
            ArtworkOrPreview(
                podcasts = podcasts,
                artworkSize = artworkSize,
            )
        }
    }
}

@Composable
private fun ArtworkOrPreview(
    podcasts: List<Podcast>,
    artworkSize: Dp,
    modifier: Modifier = Modifier,
) {
    if (!LocalInspectionMode.current) {
        PlaylistArtwork(
            podcasts = podcasts,
            artworkSize = artworkSize,
            cornerSize = 0.dp,
            modifier = modifier,
        )
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
        ) {
            previewColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color),
                )
            }
        }
    }
}

private fun Modifier.blurOrScrim(useBlur: Boolean) = if (useBlur) {
    blur(80.dp, BlurredEdgeTreatment.Unbounded)
} else {
    graphicsLayer(
        compositingStrategy = CompositingStrategy.Offscreen,
    ).drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    0.65f to Color.Black.copy(alpha = 0.5f),
                    1f to Color.Transparent,
                ),
                startY = 0f,
                endY = Float.POSITIVE_INFINITY,
            ),
            blendMode = BlendMode.DstIn,
        )
    }
}

private val artworkCrossfadeSpec = tween<Float>(durationMillis = 1000)

private val previewColors = listOf(
    Color(0xFFCC99C9),
    Color(0xFF9EC1CF),
    Color(0xFF9EE09E),
    Color(0xFFFDFD97),
    Color(0xFFFEB144),
    Color(0xFFFF6663),
    Color(0xFFCC99C9),
    Color(0xFF9EC1CF),
    Color(0xFF9EE09E),
    Color(0xFFFDFD97),
    Color(0xFFFEB144),
    Color(0xFFFF6663),
)

@PreviewRegularDevice
@Composable
private fun PlaylistHeaderNoPodcastPreview() {
    AppTheme(ThemeType.LIGHT) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.theme.colors.primaryUi02)
                .fillMaxSize(),
        ) {
            PlaylistHeader(
                data = PlaylistHeaderData(
                    title = "My Playlist",
                    artworkPodcasts = emptyList(),
                ),
                useBlurredArtwork = false,
            )
        }
    }
}

@PreviewRegularDevice
@Composable
private fun PlaylistHeaderSinglePodcastPreview() {
    AppTheme(ThemeType.LIGHT) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.theme.colors.primaryUi02)
                .fillMaxSize(),
        ) {
            PlaylistHeader(
                data = PlaylistHeaderData(
                    title = "My Playlist",
                    artworkPodcasts = listOf(Podcast(uuid = "id-0")),
                ),
                useBlurredArtwork = false,
            )
        }
    }
}

@PreviewRegularDevice
@Composable
private fun PlaylistHeaderMultiPodcastPreview() {
    AppTheme(ThemeType.LIGHT) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.theme.colors.primaryUi02)
                .fillMaxSize(),
        ) {
            PlaylistHeader(
                data = PlaylistHeaderData(
                    title = "My Playlist",
                    artworkPodcasts = List(4) { index -> Podcast(uuid = "id-$index") },
                ),
                useBlurredArtwork = false,
            )
        }
    }
}
