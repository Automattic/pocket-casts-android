package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.contextual

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP30
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class FolderPosition {
    LEFT,
    CENTER,
    RIGHT,
}

val previewTiles = (1..8).map {
    TileConfig(
        index = it,
        color = Color(
            red = Random.nextFloat(),
            green = Random.nextFloat(),
            blue = Random.nextFloat(),
            alpha = 1f,
        ),
        anchor = when (it) {
            2 -> Offset(1f, 1f)
            3 -> Offset(0f, 1f)
            6 -> Offset(1f, 0f)
            7 -> Offset(0f, 0f)
            else -> Offset(0f, 0f)
        }
    )
}

val mockTile = TileConfig(
    index = -1,
    color = Color.Transparent,
    anchor = Offset(0f, 0f),
)

data class FolderConfig(
    val position: FolderPosition,
    val folderName: String,
    val color: Color,
    val tiles: List<TileConfig>,
)

val previewFolders = listOf(
    FolderConfig(
        folderName = "Books",
        position = FolderPosition.LEFT,
        color = Color.Cyan,
        tiles = listOf(mockTile, previewTiles[0], mockTile, previewTiles[4])
    ),
    FolderConfig(
        folderName = "Favorites",
        position = FolderPosition.CENTER,
        color = Color.Blue,
        tiles = listOf(previewTiles[1], previewTiles[2], previewTiles[5], previewTiles[6])
    ),
    FolderConfig(
        folderName = "Sports",
        position = FolderPosition.RIGHT,
        color = Color.Yellow,
        tiles = listOf(previewTiles[3], mockTile, previewTiles[7], mockTile)
    ),
)


private val edgeFadeIndices = listOf(1, 4, 5, 8)

data class TileConfig(
    val index: Int,
    val color: Color,
    val anchor: Offset,
)

@ExperimentalSharedTransitionApi
@Composable
fun FoldersAnimation(
    tiles: List<TileConfig>,
    folders: List<FolderConfig>,
    modifier: Modifier = Modifier,
) {
    var edgeTilesVisible by remember { mutableStateOf(true) }
    val edgeFadeTransition = updateTransition(edgeTilesVisible, "edge tiles fade")
    val edgeFade by edgeFadeTransition.animateFloat(
        transitionSpec = {
            tween(durationMillis = 400, easing = LinearEasing)
        }, label = "alphaAnimation"
    ) { isVisible ->
        if (isVisible) {
            1f
        } else {
            0f
        }
    }
    val shrinkAnimationsIndexed = remember {
        mapOf(
            2 to Animatable(initialValue = 1f),
            3 to Animatable(initialValue = 1f),
            6 to Animatable(initialValue = 1f),
            7 to Animatable(initialValue = 1f),
        )
    }

    var showFolders by remember { mutableStateOf(false) }

    LaunchedEffect("animations") {
        edgeTilesVisible = false
        shrinkAnimationsIndexed.entries.forEachIndexed { index, entry ->
            launch {
                entry.value.animateTo(0.6f, animationSpec = tween(durationMillis = 900, easing = LinearEasing, delayMillis = index * 50))
            }
        }
        delay(1050)
        showFolders = true
    }

    // Wrap tiles in rows of 4
    SharedTransitionLayout {
        AnimatedContent(
            targetState = showFolders,
            label = "shared magic"
        ) { targetState ->
            if (targetState) {
                FolderRow(
                    left = folders.find { it.position == FolderPosition.LEFT }!!,
                    center = folders.find { it.position == FolderPosition.CENTER }!!,
                    right = folders.find { it.position == FolderPosition.RIGHT }!!,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column(
                    modifier = modifier,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tiles.chunked(4).forEach { rowTiles ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowTiles.forEach { tile ->
                                val density = LocalDensity.current
                                val boxSizePx = with(density) {
                                    80.dp.roundToPx()
                                }
                                Tile(
                                    tileConfig = tile,
                                    modifier = Modifier
                                        .then(
                                            if ((folders.find { it.position == FolderPosition.CENTER }?.tiles?.map { it.index } ?: emptyList()).contains(tile.index)) {
                                                Modifier.sharedElement(
                                                    sharedContentState = rememberSharedContentState("tile${tile.index}"),
                                                    animatedVisibilityScope = this@AnimatedContent
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .size(80.dp)
                                        .graphicsLayer {
                                            if (edgeFadeIndices.contains(tile.index)) {
                                                alpha = edgeFade
                                            }
                                            shrinkAnimationsIndexed[tile.index]?.let { shrink ->
                                                scaleX = shrink.value
                                                scaleY = shrink.value

                                                val deltaScale = shrink.value - 1f
                                                val shiftX = -deltaScale * tile.anchor.x * boxSizePx
                                                val shiftY = -deltaScale * tile.anchor.y * boxSizePx
                                                translationX = shiftX
                                                translationY = shiftY
                                            }

                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@ExperimentalSharedTransitionApi
@Composable
private fun FolderRow(
    left: FolderConfig,
    center: FolderConfig,
    right: FolderConfig,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    Layout(
        modifier = modifier,
        content = {
            Folder(spec = left, size = 132.dp, tileSize = 42.dp, sharedTransitionScope = sharedTransitionScope, animatedVisibilityScope = animatedVisibilityScope)
            Folder(spec = center, size = 219.dp, tileSize = 69.dp, sharedTransitionScope = sharedTransitionScope, animatedVisibilityScope = animatedVisibilityScope)
            Folder(spec = right, size = 132.dp, tileSize = 42.dp, sharedTransitionScope = sharedTransitionScope, animatedVisibilityScope = animatedVisibilityScope)
        }
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }

        val maxHeight = placeables.map { it.height }.maxBy { it }

        layout(constraints.maxWidth, maxHeight) {
            val horizontalCenter = constraints.maxWidth / 2

            placeables[1].placeRelative(
                x = horizontalCenter - placeables[1].width / 2,
                y = 0,
                zIndex = 1f,
            )

            placeables[0].placeRelative(
                x = -placeables[0].width / 2,
                y = 0,
                zIndex = 0.5f,
            )

            placeables[2].placeRelative(
                x = constraints.maxWidth - placeables[2].width / 2,
                y = maxHeight - placeables[2].height,
                zIndex = 0.5f,
            )

        }
    }
}

@Composable
private fun Tile(
    tileConfig: TileConfig,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color = tileConfig.color)
    ) {
        TextP30(text = tileConfig.index.toString())
    }
}

@ExperimentalSharedTransitionApi
@Composable
private fun Folder(
    spec: FolderConfig,
    size: Dp,
    tileSize: Dp,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    Column(
        modifier = Modifier
            .wrapContentSize()
            .size(size)
            .clip(RoundedCornerShape(32.dp))
            .background(color = spec.color, shape = RoundedCornerShape(32.dp))
            .padding(top = 20.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        spec.tiles.chunked(2).forEach { rowTiles ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowTiles.forEach {
                    Tile(
                        modifier = Modifier
                            .size(tileSize)
                            .then(
                                if (sharedTransitionScope == null || animatedVisibilityScope == null) {
                                    Modifier
                                } else {
                                    with(sharedTransitionScope) {
                                        Modifier.sharedElement(
                                            rememberSharedContentState("tile${it.index}"),
                                            animatedVisibilityScope = animatedVisibilityScope
                                        )
                                    }
                                }
                            ),
                        tileConfig = it
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))

        TextH30(text = spec.folderName, color = MaterialTheme.theme.colors.primaryInteractive02)
    }
}

@ExperimentalSharedTransitionApi
@Preview
@Composable
private fun PreviewFolder(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) = AppTheme(theme) {
    Box(contentAlignment = Alignment.Center) {
        Folder(
            size = 219.dp,
            spec = FolderConfig(
                position = FolderPosition.CENTER,
                folderName = "Favorites",
                color = Color.Blue,
                tiles = previewTiles.take(4)
            ),
            tileSize = 69.dp,
            sharedTransitionScope = null,
            animatedVisibilityScope = null
        )
    }
}


@ExperimentalSharedTransitionApi
@Preview
@Composable
private fun PreviewFolderRow(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) = AppTheme(theme) {
    FolderRow(
        left = previewFolders.find { it.position == FolderPosition.LEFT }!!,
        center = previewFolders.find { it.position == FolderPosition.CENTER }!!,
        right = previewFolders.find { it.position == FolderPosition.RIGHT }!!,
        modifier = Modifier.fillMaxWidth()
    )
}
