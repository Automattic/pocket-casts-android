package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import coil3.compose.AsyncImage

private val CardShape = RoundedCornerShape(9.dp)
private val CoverSize = 78.dp
private val CoverShape = RoundedCornerShape(6.dp)
private const val COVER_VISIBLE_FRACTION = 0.55f
private const val COVER_RESTING_SCALE = 0.85f

@Composable
fun TvCategoryTile(
    category: DiscoverCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colorIndex: Int = 0,
    loadCoverUrls: (suspend () -> List<String>)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val contentColor = if (isFocused) MaterialTheme.tvColors.textPrimary else MaterialTheme.tvColors.textSecondary

    val loader by rememberUpdatedState(loadCoverUrls)
    var coverUrls by remember(category.id) { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(category.id, isFocused) {
        if (isFocused && coverUrls.isEmpty()) {
            coverUrls = loader?.invoke() ?: emptyList()
        }
    }

    val focusProgress = animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "TvCategoryTileFocus",
    )
    val coverSizePx = with(LocalDensity.current) { CoverSize.toPx() }
    val directionSign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f

    TvTile(
        onClick = onClick,
        shape = CardDefaults.shape(shape = CardShape),
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.tvColors.backgroundOverlay,
            focusedContainerColor = MaterialTheme.tvColors.backgroundOverlay,
        ),
        glow = CardDefaults.glow(
            focusedGlow = Glow(elevationColor = Color.Black.copy(alpha = 0.5f), elevation = 6.dp),
        ),
        interactionSource = interactionSource,
        modifier = modifier
            .width(284.dp)
            .height(129.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = focusProgress.value.coerceIn(0f, 1f) }
                    .background(TvCategoryStyle.gradient(colorIndex)),
            )

            coverUrls.firstOrNull()?.let { url ->
                CategoryCover(
                    url = url,
                    alignment = Alignment.CenterStart,
                    edgeSign = -1f * directionSign,
                    focusProgress = { focusProgress.value },
                    coverSizePx = coverSizePx,
                )
            }
            coverUrls.getOrNull(1)?.let { url ->
                CategoryCover(
                    url = url,
                    alignment = Alignment.CenterEnd,
                    edgeSign = 1f * directionSign,
                    focusProgress = { focusProgress.value },
                    coverSizePx = coverSizePx,
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                AsyncImage(
                    model = category.icon,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(contentColor),
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.tvTypography.headline,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 9.dp),
                )
            }
        }
    }
}

@Composable
private fun BoxScope.CategoryCover(
    url: String,
    alignment: Alignment,
    edgeSign: Float,
    focusProgress: () -> Float,
    coverSizePx: Float,
) {
    Box(
        modifier = Modifier
            .align(alignment)
            .size(CoverSize)
            .graphicsLayer {
                val progress = focusProgress().coerceIn(0f, 1f)
                alpha = progress
                val scale = COVER_RESTING_SCALE + (1f - COVER_RESTING_SCALE) * progress
                scaleX = scale
                scaleY = scale
                translationX = edgeSign * coverSizePx * (1f - COVER_VISIBLE_FRACTION * progress)
            },
    ) {
        TvArtworkImage(
            model = url,
            modifier = Modifier
                .fillMaxSize()
                .clip(CoverShape),
        )
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvCategoryTilePreview() {
    TvTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.tvColors.backgroundSunken)
                .padding(36.dp),
        ) {
            TvCategoryTile(
                category = DiscoverCategory(id = 1, name = "Comedy", icon = "", source = ""),
                onClick = {},
                colorIndex = 0,
            )
        }
    }
}
