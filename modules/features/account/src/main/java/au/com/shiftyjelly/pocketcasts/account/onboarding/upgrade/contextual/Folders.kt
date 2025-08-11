package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.contextual

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.Util
import kotlin.math.min
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private data class TileConfig(
    val index: Int,
    @DrawableRes val drawableResId: Int?,
    val anchor: TransformOrigin,
    val isElevated: Boolean = false,
)

private data class FolderConfig(
    @StringRes val folderNameRes: Int,
    val color: Color,
    val tiles: List<TileConfig>,
)

private val predefinedTiles = (1..8).map {
    TileConfig(
        index = it,
        drawableResId = when (it) {
            1 -> IR.drawable.artwork_0
            2 -> IR.drawable.artwork_2
            3 -> IR.drawable.artwork_3
            4 -> IR.drawable.artwork_6
            5 -> IR.drawable.artwork_1
            6 -> IR.drawable.artwork_4
            7 -> IR.drawable.artwork_5
            else -> IR.drawable.artwork_7
        },
        anchor = when (it) {
            2 -> TransformOrigin(1f, 1f)
            3 -> TransformOrigin(0f, 1f)
            6 -> TransformOrigin(1f, 0f)
            7 -> TransformOrigin(0f, 0f)
            else -> TransformOrigin(0f, 0f)
        },
        isElevated = it == 2,
    )
}

private val placeholderTile = TileConfig(
    index = -1,
    drawableResId = null,
    anchor = TransformOrigin(0f, 0f),
)

@ReadOnlyComposable
@Composable
private fun predefinedFolders() = listOf(
    FolderConfig(
        folderNameRes = LR.string.onboarding_folders_title_1,
        color = Color(0xFF9BA2FF),
        tiles = listOf(placeholderTile, predefinedTiles[0], placeholderTile, predefinedTiles[4]),
    ),
    FolderConfig(
        folderNameRes = LR.string.onboarding_folders_title_2,
        color = MaterialTheme.theme.colors.primaryInteractive01,
        tiles = List(4) { placeholderTile },
    ),
    FolderConfig(
        folderNameRes = LR.string.onboarding_folders_title_3,
        color = Color(0xFF32D9A9),
        tiles = listOf(predefinedTiles[3], placeholderTile, predefinedTiles[7], placeholderTile),
    ),
)

private val edgeFadeIndices = listOf(1, 4, 5, 8)

@Composable
fun FoldersAnimation(
    modifier: Modifier = Modifier,
) {
    val tiles: List<TileConfig> = predefinedTiles
    val folders: List<FolderConfig> = predefinedFolders()
    var showFolders by remember { mutableStateOf(false) }

    LaunchedEffect("showFolders") {
        delay(600)
        showFolders = true
    }

    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) { role = Role.Image }
            .focusable(false),
        contentAlignment = Alignment.Center,
    ) {
        if (showFolders) {
            FolderRow(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxHeight(),
                left = folders[0],
                center = folders[1],
                right = folders[2],
            )
        }
        TileRows(
            tiles = tiles,
            modifier = Modifier.fillMaxHeight(),
        )
    }
}

@Composable
private fun TileRows(
    tiles: List<TileConfig>,
    modifier: Modifier = Modifier,
    animationDelayMillis: Int = 600,
) {
    var edgeTilesVisible by remember { mutableStateOf(true) }
    val edgeFadeTransition = updateTransition(edgeTilesVisible, "edge tiles fade")
    val edgeFade by edgeFadeTransition.animateFloat(
        transitionSpec = {
            tween(durationMillis = 400, easing = FastOutLinearInEasing, delayMillis = animationDelayMillis)
        },
        label = "alphaAnimation",
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

    LaunchedEffect("animations") {
        edgeTilesVisible = false
        shrinkAnimationsIndexed.entries.forEachIndexed { index, entry ->
            launch {
                entry.value.animateTo(
                    0.6f,
                    animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing, delayMillis = index * 60 + animationDelayMillis + 150),
                )
            }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        val rows = tiles.chunked(4)
        rows.forEachIndexed { index, rowTiles ->
            TruncatedRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                rowTiles.forEach { tile ->
                    Tile(
                        tileConfig = tile,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .graphicsLayer {
                                if (edgeFadeIndices.contains(tile.index)) {
                                    alpha = edgeFade
                                }
                                shrinkAnimationsIndexed[tile.index]?.let { shrink ->
                                    scaleX = shrink.value
                                    scaleY = shrink.value
                                    transformOrigin = tile.anchor
                                }
                            },
                    )
                }
            }
            if (index != rows.lastIndex) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun TruncatedRow(
    modifier: Modifier = Modifier,
    peekWidth: Dp = 32.dp,
    spacing: Dp = 12.dp,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        content = content,
    ) { measurables, constraints ->
        val itemCount = measurables.size
        if (itemCount == 0) {
            layout(0, 0) {}
        } else {
            val peekPx = peekWidth.roundToPx()
            val spacingPx = spacing.roundToPx()
            val totalSpacing = spacingPx * (itemCount - 1)
            val availableWidth = constraints.maxWidth + (2 * peekPx)
            val itemWidth = (availableWidth - totalSpacing) / itemCount
            val placeables = measurables.map {
                it.measure(Constraints.fixedWidth(itemWidth))
            }
            val layoutHeight = placeables.maxOf { it.height }

            layout(constraints.maxWidth, layoutHeight) {
                var x = -peekPx

                placeables.forEach { placeable ->
                    placeable.placeRelative(x, 0)
                    x += itemWidth + spacingPx
                }
            }
        }
    }
}

@Composable
private fun FolderRow(
    left: FolderConfig,
    center: FolderConfig,
    right: FolderConfig,
    modifier: Modifier = Modifier,
    spaceBetweenFolders: Dp = 32.dp,
    edgeFolderSizePercent: Int = 70,
    showFolders: Boolean = false,
) {
    val animationStarters = remember { List(3) { mutableStateOf(showFolders) } }

    LaunchedEffect("animations") {
        animationStarters[1].value = true
        delay(400L)
        animationStarters[0].value = true
        delay(20L)
        animationStarters[2].value = true
    }

    Layout(
        modifier = modifier,
        content = {
            Folder(
                modifier = Modifier.aspectRatio(1f),
                spec = left,
                startTransition = animationStarters[0].value,
                alphaTargetValue = 0.33f,
                scaleFactor = edgeFolderSizePercent / 100f,
            )
            Folder(
                modifier = Modifier.aspectRatio(1f),
                spec = center,
                startTransition = animationStarters[1].value,
                floatInOffset = 24.dp,
                animationDurationMillis = 300,
                easing = LinearEasing,
                animationDelayMillis = 400,
            )
            Folder(
                modifier = Modifier.aspectRatio(1f),
                spec = right,
                startTransition = animationStarters[2].value,
                alphaTargetValue = 0.66f,
                scaleFactor = edgeFolderSizePercent / 100f,
            )
        },
    ) { measurables, constraints ->
        val spacingPx = spaceBetweenFolders.roundToPx()
        val itemCount = 3
        val totalSpacing = spacingPx * (itemCount - 1)

        val centerItemSize = (constraints.maxWidth - totalSpacing) / (1 + (edgeFolderSizePercent / 100f))
        val edgeItemSize = centerItemSize * (edgeFolderSizePercent / 100f)
        val placeables = measurables.mapIndexed { index, item ->
            if (index == 1) {
                item.measure(Constraints.fixedWidth(centerItemSize.toInt()))
            } else {
                item.measure(Constraints.fixedWidth(edgeItemSize.toInt()))
            }
        }

        val maxHeight = placeables.maxOf { it.height }
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
    tileConfig.drawableResId?.let {
        Image(
            modifier = modifier
                .then(
                    if (tileConfig.isElevated) {
                        Modifier.graphicsLayer {
                            shadowElevation = 8.dp.toPx()
                            shape = RoundedCornerShape(8.dp)
                            clip = false
                        }
                    } else {
                        Modifier
                    },
                )
                .clip(RoundedCornerShape(8.dp)),
            painter = painterResource(it),
            contentDescription = "",
        )
    } ?: Box(modifier = modifier)
}

@Composable
private fun Folder(
    spec: FolderConfig,
    startTransition: Boolean,
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    floatInOffset: Dp = 64.dp,
    titleTopPadding: Dp = 14.dp,
    animationDurationMillis: Int = 900,
    animationDelayMillis: Int = 0,
    easing: Easing = FastOutSlowInEasing,
    alphaTargetValue: Float = 1f,
    scaleFactor: Float = 1f,
) {
    val floatInOffsetPx = LocalDensity.current.run {
        floatInOffset.toPx()
    }
    val transition = updateTransition(startTransition)
    val alphaAnim by transition.animateFloat(
        transitionSpec = {
            tween(durationMillis = animationDurationMillis, easing = easing, delayMillis = animationDelayMillis)
        },
    ) { isVisible ->
        if (isVisible) {
            alphaTargetValue
        } else {
            0f
        }
    }
    val translationYAnim by transition.animateFloat(
        transitionSpec = {
            tween(durationMillis = animationDurationMillis, easing = easing, delayMillis = animationDelayMillis)
        },
    ) { isVisible ->
        if (isVisible) {
            0f
        } else {
            floatInOffsetPx
        }
    }

    val titleSize = if (Util.isTablet(LocalContext.current)) {
        26.sp
    } else {
        18.sp
    }

    Layout(
        modifier = modifier
            .graphicsLayer {
                alpha = alphaAnim
                translationY = translationYAnim
            }
            .wrapContentSize()
            .clip(RoundedCornerShape(32.dp * scaleFactor))
            .background(color = spec.color, shape = RoundedCornerShape(32.dp * scaleFactor))
            .background(brush = Brush.linearGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = .2f))))
            .padding(top = 20.dp * scaleFactor, bottom = 12.dp * scaleFactor)
            .padding(horizontal = 20.dp * scaleFactor),
        content = {
            spec.tiles.take(4).forEach {
                Tile(
                    modifier = Modifier.aspectRatio(1f),
                    tileConfig = it,
                )
            }
            TextH30(
                text = stringResource(spec.folderNameRes),
                color = MaterialTheme.theme.colors.primaryInteractive02,
                maxLines = 1,
                disableAutoScale = true,
                fontScale = scaleFactor,
                fontSize = titleSize,
            )
        },
    ) { measurables, constraints ->
        val spacingPx = (spacing * scaleFactor).roundToPx()
        val itemSizeHorizontal = (constraints.maxWidth - spacingPx) / 2
        val itemSizeVertical = (constraints.maxHeight - spacingPx) / 2 - (titleTopPadding * scaleFactor).roundToPx()
        val itemSize = min(itemSizeHorizontal, itemSizeVertical)
        val placeables = measurables.mapIndexed { index, item ->
            if (index >= 4) {
                item.measure(constraints)
            } else {
                item.measure(Constraints.fixedWidth(itemSize))
            }
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { index, item ->
                if (index >= 4) {
                    item.placeRelative(
                        x = (constraints.maxWidth - item.width) / 2,
                        y = constraints.maxHeight - item.height,
                    )
                } else {
                    val row = index / 2
                    val col = index % 2
                    val centerX = constraints.maxWidth / 2

                    item.placeRelative(
                        x = centerX + if (col == 0) -spacingPx / 2 - item.width else spacingPx / 2,
                        y = row * (item.height + spacingPx),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewFolder(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) = AppTheme(theme) {
    Column {
        Folder(
            modifier = Modifier.size(219.dp),
            spec = FolderConfig(
                folderNameRes = LR.string.onboarding_folders_title_1,
                color = Color.Blue,
                tiles = predefinedTiles.take(4),
            ),
            startTransition = true,
        )
        Spacer(Modifier.height(12.dp))
        Folder(
            modifier = Modifier.size(120.dp),
            spec = FolderConfig(
                folderNameRes = LR.string.onboarding_folders_title_3,
                color = Color.Blue,
                tiles = predefinedTiles.take(4),
            ),
            startTransition = true,
            scaleFactor = .6f,
        )
    }
}

@Preview
@Composable
private fun PreviewTileRows(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) = AppTheme(theme) {
    TileRows(
        modifier = Modifier.fillMaxWidth(),
        tiles = predefinedTiles,
    )
}

@Preview
@Composable
private fun PreviewFolderRow(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) = AppTheme(theme) {
    Column {
        val previewFolders = predefinedFolders()
        FolderRow(
            modifier = Modifier.fillMaxWidth(),
            left = previewFolders[0],
            center = previewFolders[1],
            right = previewFolders[2],
            showFolders = true,
        )
    }
}

@Preview
@Composable
private fun PreviewTruncatedRow(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) = AppTheme(theme) {
    TruncatedRow {
        (0..4).map {
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(randomColor()),
            )
        }
    }
}

private fun randomColor() = Color(
    red = Random.nextFloat(),
    green = Random.nextFloat(),
    blue = Random.nextFloat(),
    alpha = 1f,
)
