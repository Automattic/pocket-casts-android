package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.annotation.FloatRange
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun TooltipPopup(
    title: String,
    tipPosition: TipPosition,
    body: String? = null,
    @FloatRange(from = 0.0, to = 1.0) maxWidthFraction: Float? = null,
    maxWidth: Dp? = null,
    elevation: Dp = 16.dp,
    anchorOffset: DpOffset = DpOffset.Zero,
    properties: PopupProperties = PopupProperties(dismissOnClickOutside = false),
    onClick: (() -> Unit)? = null,
    onClickOutside: (() -> Unit)? = null,
) {
    // We're adding additional padding to the popup box in order to prevent shadow clipping.
    // There's no API to determine how much padding is needed. We just eyeball it to 150%.
    val elevationPadding = elevation * 1.5f
    Popup(
        popupPositionProvider = rememberTooltipPositionProvider(tipPosition, anchorOffset, elevationPadding),
        properties = properties,
        onDismissRequest = onClickOutside,
    ) {
        Box(
            modifier = Modifier.padding(elevationPadding),
        ) {
            Tooltip(
                title = title,
                body = body,
                tipPosition = tipPosition,
                elevation = elevation,
                modifier = Modifier
                    .then(
                        if (maxWidthFraction != null) {
                            Modifier.fillMaxWidth(maxWidthFraction)
                        } else {
                            Modifier
                        },
                    )
                    .then(
                        if (maxWidth != null) {
                            Modifier.widthIn(max = maxWidth)
                        } else {
                            Modifier
                        },
                    )
                    .then(
                        if (onClick != null) {
                            Modifier.clickable(onClick = onClick)
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}

@Composable
fun Tooltip(
    title: String,
    tipPosition: TipPosition,
    modifier: Modifier = Modifier,
    body: String? = null,
    elevation: Dp = 16.dp,
) {
    val tooltipShape = TooltipShape(tipPosition)
    val backgroundColor = when (MaterialTheme.theme.type) {
        Theme.ThemeType.DARK_CONTRAST -> MaterialTheme.theme.colors.primaryUi05
        else -> MaterialTheme.theme.colors.primaryUi01
    }

    Box(
        modifier = (if (elevation > 0.dp) Modifier.shadow(elevation, tooltipShape) else Modifier)
            .clip(tooltipShape)
            .background(backgroundColor)
            .then(modifier),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            if (tipPosition.isTopAligned()) {
                Spacer(
                    modifier = Modifier.height(TipHeight),
                )
            }
            TextH40(
                text = title,
            )
            if (body != null) {
                Spacer(
                    modifier = Modifier.height(4.dp),
                )
                TextP50(
                    text = body,
                    color = MaterialTheme.theme.colors.primaryText02,
                )
            }
            if (tipPosition.isBottomAligned()) {
                Spacer(
                    modifier = Modifier.height(TipHeight),
                )
            }
        }
    }
}

enum class TipPosition {
    TopStart,
    TopCenter,
    TopEnd,
    BottomStart,
    BottomCenter,
    BottomEnd,
    ;

    internal fun isTopAligned() = this == TopStart || this == TopCenter || this == TopEnd

    internal fun isBottomAligned() = !isTopAligned()

    internal fun normalize(layoutDirection: LayoutDirection) = when (this) {
        TopCenter -> TopCenter
        BottomCenter -> BottomCenter
        TopStart -> when (layoutDirection) {
            LayoutDirection.Ltr -> TopStart
            LayoutDirection.Rtl -> TopEnd
        }

        TopEnd -> when (layoutDirection) {
            LayoutDirection.Ltr -> TopEnd
            LayoutDirection.Rtl -> TopStart
        }

        BottomStart -> when (layoutDirection) {
            LayoutDirection.Ltr -> BottomStart
            LayoutDirection.Rtl -> BottomEnd
        }

        BottomEnd -> when (layoutDirection) {
            LayoutDirection.Ltr -> BottomEnd
            LayoutDirection.Rtl -> BottomStart
        }
    }
}

@Composable
private fun rememberTooltipPositionProvider(
    tipPosition: TipPosition,
    anchorOffset: DpOffset,
    elevation: Dp,
): PopupPositionProvider {
    val density = LocalDensity.current
    return remember(tipPosition, anchorOffset, elevation, density) {
        TooltipPositionProvider(tipPosition, anchorOffset, elevation, density)
    }
}

private class TooltipPositionProvider(
    private val tipPosition: TipPosition,
    anchorOffset: DpOffset,
    private val elevation: Dp,
    private val density: Density,
) : PopupPositionProvider {
    private val anchorOffset = density.run {
        IntOffset(anchorOffset.x.roundToPx(), anchorOffset.y.roundToPx())
    }

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val elevationPx = density.run { elevation.roundToPx() }

        return anchorOffset + when (tipPosition.normalize(layoutDirection)) {
            TipPosition.TopStart -> {
                val elevationOffset = IntOffset(elevationPx, elevationPx)
                val tipOffset = IntOffset(density.run { CornerTipPeakPosition.roundToPx() }, 0)
                anchorBounds.bottomCenter - elevationOffset - tipOffset
            }

            TipPosition.TopCenter -> {
                val elevationOffset = IntOffset(0, elevationPx)
                val contentOffset = IntOffset(popupContentSize.width / 2, 0)
                anchorBounds.bottomCenter - elevationOffset - contentOffset
            }

            TipPosition.TopEnd -> {
                val elevationOffset = IntOffset(-elevationPx, elevationPx)
                val contentOffset = IntOffset(popupContentSize.width, 0)
                val tipOffset = IntOffset(density.run { CornerTipPeakPosition.roundToPx() }, 0)
                anchorBounds.bottomCenter - elevationOffset - contentOffset + tipOffset
            }

            TipPosition.BottomStart -> {
                val elevationOffset = IntOffset(elevationPx, -elevationPx)
                val contentOffset = IntOffset(0, popupContentSize.height)
                val tipOffset = IntOffset(density.run { CornerTipPeakPosition.roundToPx() }, 0)
                anchorBounds.topCenter - elevationOffset - contentOffset - tipOffset
            }

            TipPosition.BottomCenter -> {
                val elevationOffset = IntOffset(0, -elevationPx)
                val contentOffset = IntOffset(popupContentSize.width / 2, popupContentSize.height)
                anchorBounds.topCenter - elevationOffset - contentOffset
            }

            TipPosition.BottomEnd -> {
                val elevationOffset = IntOffset(elevationPx, elevationPx)
                val contentOffset = IntOffset(popupContentSize.width, popupContentSize.height)
                val tipOffset = IntOffset(density.run { CornerTipPeakPosition.roundToPx() }, 0)
                anchorBounds.topCenter + elevationOffset - contentOffset + tipOffset
            }
        }
    }
}

private class TooltipShape(
    private val tipPosition: TipPosition,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val cardPath = roundedRectPath(size, density)
        val tipPath = tipPath(size, layoutDirection, density)
        val path = Path.combine(PathOperation.Union, cardPath, tipPath)

        return Outline.Generic(path)
    }

    private fun roundedRectPath(
        size: Size,
        density: Density,
    ): Path {
        val tipHeight = density.run { TipHeight.toPx() }
        val cornerRadius = density.run { CornerRadius.toPx() }
        val topOffset = if (tipPosition.isTopAligned()) tipHeight else 0f
        val bottomOffset = if (tipPosition.isBottomAligned()) tipHeight else 0f

        return Path().apply {
            val roundRect = RoundRect(
                left = 0f,
                top = topOffset,
                right = size.width,
                bottom = size.height - bottomOffset,
                radiusX = cornerRadius,
                radiusY = cornerRadius,
            )
            addRoundRect(roundRect)
        }
    }

    private fun tipPath(
        shapeSize: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ) = when (tipPosition.normalize(layoutDirection)) {
        TipPosition.TopCenter -> topTip(shapeSize, density)
        TipPosition.BottomCenter -> bottomTip(shapeSize, density)
        TipPosition.TopStart -> topLeftTip(density)
        TipPosition.TopEnd -> topRightTip(shapeSize, density)
        TipPosition.BottomStart -> bottomLeftTip(shapeSize, density)
        TipPosition.BottomEnd -> bottomRightTip(shapeSize, density)
    }

    private fun topTip(
        shapeSize: Size,
        density: Density,
    ) = Path().apply {
        val scale = density.density
        val tipHeight = density.run { TipHeight.toPx() }
        val tipWidth = density.run { CenterTipWidth.toPx() }

        moveTo(shapeSize.width / 2 - tipWidth / 2, tipHeight)
        addEdgeTip(scale)
    }

    private fun bottomTip(
        shapeSize: Size,
        density: Density,
    ) = Path().apply {
        val scale = density.density
        val tipHeight = density.run { TipHeight.toPx() }
        val tipWidth = density.run { CenterTipWidth.toPx() }

        moveTo(shapeSize.width / 2 - tipWidth / 2, shapeSize.height - tipHeight)
        addEdgeTip(scale, flipVertically = true)
    }

    private fun topRightTip(
        shapeSize: Size,
        density: Density,
    ) = Path().apply {
        val scale = density.density
        val tipHeight = density.run { TipHeight.toPx() }
        val tipWidth = density.run { CornerTipWidth.toPx() }

        moveTo(shapeSize.width - tipWidth, tipHeight)
        addCornerTip(scale)
    }

    private fun topLeftTip(
        density: Density,
    ) = Path().apply {
        val scale = density.density
        val tipHeight = density.run { TipHeight.toPx() }
        val tipWidth = density.run { CornerTipWidth.toPx() }

        moveTo(tipWidth, tipHeight)
        addCornerTip(scale, flipHorizontally = true)
    }

    private fun bottomRightTip(
        shapeSize: Size,
        density: Density,
    ) = Path().apply {
        val scale = density.density
        val tipHeight = density.run { TipHeight.toPx() }
        val tipWidth = density.run { CornerTipWidth.toPx() }

        moveTo(shapeSize.width - tipWidth, shapeSize.height - tipHeight)
        addCornerTip(scale, flipVertically = true)
    }

    private fun bottomLeftTip(
        shapeSize: Size,
        density: Density,
    ) = Path().apply {
        val scale = density.density
        val tipHeight = density.run { TipHeight.toPx() }
        val tipWidth = density.run { CornerTipWidth.toPx() }

        moveTo(tipWidth, shapeSize.height - tipHeight)
        addCornerTip(scale, flipHorizontally = true, flipVertically = true)
    }

    private fun Path.addEdgeTip(
        scale: Float,
        flipVertically: Boolean = false,
    ) {
        val hScale = scale
        val vScale = scale * (if (flipVertically) -1 else 1)

        relativeCubicTo(1.71f * hScale, 0f * vScale, 3.42f * hScale, 0.01f * vScale, 5.14f * hScale, -0.01f * vScale)
        relativeCubicTo(1.62f * hScale, -0.02f * vScale, 3.47f * hScale, -0.04f * vScale, 5.07f * hScale, -0.65f * vScale)
        relativeCubicTo(1.72f * hScale, -0.65f * vScale, 2.86f * hScale, -1.79f * vScale, 4.01f * hScale, -3.12f * vScale)
        relativeCubicTo(0.83f * hScale, -0.96f * vScale, 2.45f * hScale, -3.01f * vScale, 3.24f * hScale, -4.01f * vScale)
        relativeCubicTo(0.65f * hScale, -0.81f * vScale, 1.91f * hScale, -2.42f * vScale, 2.6f * hScale, -3.2f * vScale)
        relativeCubicTo(0.87f * hScale, -0.98f * vScale, 1.95f * hScale, -2.02f * vScale, 3.44f * hScale, -2.02f * vScale)
        relativeCubicTo(1.49f * hScale, 0f * vScale, 2.57f * hScale, 1.04f * vScale, 3.44f * hScale, 2.02f * vScale)
        relativeCubicTo(0.69f * hScale, 0.78f * vScale, 1.96f * hScale, 2.38f * vScale, 2.6f * hScale, 3.2f * vScale)
        relativeCubicTo(0.79f * hScale, 0.99f * vScale, 2.41f * hScale, 3.04f * vScale, 3.24f * hScale, 4.01f * vScale)
        relativeCubicTo(1.15f * hScale, 1.34f * vScale, 2.29f * hScale, 2.47f * vScale, 4.01f * hScale, 3.12f * vScale)
        relativeCubicTo(1.6f * hScale, 0.6f * vScale, 3.43f * hScale, 0.63f * vScale, 5.07f * hScale, 0.65f * vScale)
        relativeCubicTo(1.71f * hScale, 0.02f * vScale, 3.43f * hScale, 0.01f * vScale, 5.14f * hScale, 0.01f * vScale)
    }

    private fun Path.addCornerTip(
        scale: Float,
        flipHorizontally: Boolean = false,
        flipVertically: Boolean = false,
    ) {
        val hScale = scale * (if (flipHorizontally) -1 else 1)
        val vScale = scale * (if (flipVertically) -1 else 1)

        relativeCubicTo(1.71f * hScale, 0f * vScale, 3.42f * hScale, 0.01f * vScale, 5.14f * hScale, -0.01f * vScale)
        relativeCubicTo(1.62f * hScale, -0.02f * vScale, 3.47f * hScale, -0.04f * vScale, 5.07f * hScale, -0.65f * vScale)
        relativeCubicTo(1.72f * hScale, -0.65f * vScale, 2.86f * hScale, -1.79f * vScale, 4.01f * hScale, -3.12f * vScale)
        relativeCubicTo(0.83f * hScale, -0.96f * vScale, 2.45f * hScale, -3.01f * vScale, 3.24f * hScale, -4.01f * vScale)
        relativeCubicTo(0.65f * hScale, -0.81f * vScale, 1.91f * hScale, -2.42f * vScale, 2.6f * hScale, -3.2f * vScale)
        relativeCubicTo(0.87f * hScale, -0.98f * vScale, 1.95f * hScale, -2.02f * vScale, 3.44f * hScale, -2.02f * vScale)
        relativeCubicTo(1.49f * hScale, 0f * vScale, 2.57f * hScale, 1.04f * vScale, 3.44f * hScale, 2.02f * vScale)
        relativeCubicTo(0.69f * hScale, 0.78f * vScale, 1.96f * hScale, 2.38f * vScale, 2.6f * hScale, 3.2f * vScale)
        relativeCubicTo(0.79f * hScale, 0.99f * vScale, 2.41f * hScale, 3.04f * vScale, 3.24f * hScale, 4.01f * vScale)
        relativeCubicTo(7.22f * hScale, 8.4f * vScale, 7.22f * hScale, 8.4f * vScale, 7.22f * hScale, 20.28f * vScale)
    }
}

private val TipHeight = 13.dp
private val CornerTipWidth = 40.dp
private val CornerTipPeakPosition = 16.5.dp
private val CenterTipWidth = 47.dp
private val CornerRadius = 10.dp

@Preview
@Composable
private fun TooltipThemePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        Box(
            modifier = Modifier.padding(16.dp),
        ) {
            Tooltip(
                title = "Sort by “Recently Played”",
                body = "You can now sort by Recently Played and quickly pick up where you left off.",
                tipPosition = TipPosition.BottomStart,
                modifier = Modifier.widthIn(max = 300.dp),
            )
        }
    }
}

@Preview
@Composable
private fun TooltipTipPreview(
    @PreviewParameter(TipPositionParameterProvider::class) tipPosition: TipPosition,
) {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        Box(
            modifier = Modifier.padding(16.dp),
        ) {
            Tooltip(
                title = "Sort by “Recently Played”",
                body = "You can now sort by Recently Played and quickly pick up where you left off.",
                tipPosition = tipPosition,
                modifier = Modifier.widthIn(max = 300.dp),
            )
        }
    }
}

private class TipPositionParameterProvider : PreviewParameterProvider<TipPosition> {
    override val values = TipPosition.entries.asSequence()
}
