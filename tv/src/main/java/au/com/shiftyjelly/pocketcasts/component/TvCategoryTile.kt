package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import coil3.compose.AsyncImage

private val CardShape = RoundedCornerShape(12.dp)
private val CoverSize = 96.dp
private val CoverShape = RoundedCornerShape(8.dp)
private const val CoverVisibleFraction = 0.55f
private const val CoverRestingScale = 0.85f

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
    val coverUrls by produceState(initialValue = emptyList<String>(), category.id) {
        value = loader?.let { runCatching { it() }.getOrDefault(emptyList()) }.orEmpty()
    }

    val focusProgress by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "TvCategoryTileFocus",
    )
    val coverSizePx = with(LocalDensity.current) { CoverSize.toPx() }

    TvTile(
        onClick = onClick,
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.tvColors.backgroundOverlay,
            focusedContainerColor = MaterialTheme.tvColors.backgroundOverlay,
        ),
        interactionSource = interactionSource,
        modifier = modifier
            .width(280.dp)
            .height(128.dp)
            .tvFocusedCardDepth(isFocused, CardShape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CardShape),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = focusProgress }
                    .background(categoryGradient(colorIndex)),
            )

            coverUrls.firstOrNull()?.let { url ->
                CategoryCover(
                    url = url,
                    alignment = Alignment.CenterStart,
                    edgeSign = -1f,
                    focusProgress = focusProgress,
                    coverSizePx = coverSizePx,
                )
            }
            coverUrls.takeIf { it.size > 1 }?.last()?.let { url ->
                CategoryCover(
                    url = url,
                    alignment = Alignment.CenterEnd,
                    edgeSign = 1f,
                    focusProgress = focusProgress,
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
                    modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = category.name,
                    style = MaterialTheme.tvTypography.body,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun CategoryCover(
    url: String,
    alignment: Alignment,
    edgeSign: Float,
    focusProgress: Float,
    coverSizePx: Float,
) {
    Box(
        modifier = Modifier
            .align(alignment)
            .size(CoverSize)
            .graphicsLayer {
                alpha = focusProgress
                val scale = CoverRestingScale + (1f - CoverRestingScale) * focusProgress
                scaleX = scale
                scaleY = scale
                translationX = edgeSign * coverSizePx * (1f - CoverVisibleFraction * focusProgress)
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
                .padding(48.dp),
        ) {
            TvCategoryTile(
                category = DiscoverCategory(id = 1, name = "Comedy", icon = "", source = ""),
                onClick = {},
                colorIndex = 0,
            )
        }
    }
}
