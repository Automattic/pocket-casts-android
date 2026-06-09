package au.com.shiftyjelly.pocketcasts.onboarding.welcome

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.images.R as IR

private const val TILE_SIZE_DP = 145
private const val TILE_SPACING_DP = 9
private const val TILE_CORNER_RADIUS_DP = 6
private const val ANIMATION_OFFSET_DP = 107
private const val GRID_VERTICAL_OFFSET_DP = -73
private const val ANIMATION_DURATION_MS = 20_000
private const val ROW_COUNT = 3
private const val TILES_PER_ROW = 10

private val ArtworkResIds = listOf(
    IR.drawable.artwork_0,
    IR.drawable.artwork_1,
    IR.drawable.artwork_2,
    IR.drawable.artwork_3,
    IR.drawable.artwork_4,
    IR.drawable.artwork_5,
    IR.drawable.artwork_6,
    IR.drawable.artwork_7,
    IR.drawable.artwork_8,
    IR.drawable.artwork_10,
    IR.drawable.artwork_11,
    IR.drawable.artwork_12,
    IR.drawable.artwork_13,
    IR.drawable.artwork_14,
    IR.drawable.artwork_15,
    IR.drawable.artwork_16,
)

@Composable
internal fun TvAnimatedPodcastGrid(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "podcast_grid")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = ANIMATION_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "grid_offset",
    )

    val offsetPx = with(LocalDensity.current) { ANIMATION_OFFSET_DP.dp.toPx() }

    val rows = remember {
        (0 until ROW_COUNT).map { rowIndex ->
            (0 until TILES_PER_ROW).map { tileIndex ->
                ArtworkResIds[(rowIndex * TILES_PER_ROW + tileIndex) % ArtworkResIds.size]
            }
        }
    }

    Column(
        modifier = modifier
            .clipToBounds()
            .offset(y = GRID_VERTICAL_OFFSET_DP.dp),
        verticalArrangement = Arrangement.spacedBy(TILE_SPACING_DP.dp),
    ) {
        rows.forEachIndexed { rowIndex, artworks ->
            val direction = if (rowIndex % 2 == 0) 1f else -1f
            val translationX = animationProgress * offsetPx * direction

            Row(
                horizontalArrangement = Arrangement.spacedBy(TILE_SPACING_DP.dp),
                modifier = Modifier
                    .wrapContentWidth(unbounded = true)
                    .graphicsLayer { this.translationX = translationX },
            ) {
                artworks.forEach { resId ->
                    Image(
                        painter = painterResource(resId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(TILE_SIZE_DP.dp)
                            .clip(RoundedCornerShape(TILE_CORNER_RADIUS_DP.dp)),
                    )
                }
            }
        }
    }
}
