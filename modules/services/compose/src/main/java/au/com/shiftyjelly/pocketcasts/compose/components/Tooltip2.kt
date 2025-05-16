package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun Tooltip2(
    title: String,
    anchorPosition: AnchorPosition,
    modifier: Modifier = Modifier,
    body: String? = null,
    elevation: Dp = 16.dp,
) {
    val tooltipShape = Tooltip2Shape(anchorPosition)
    val backgroundColor = when (MaterialTheme.theme.type) {
        Theme.ThemeType.DARK_CONTRAST -> MaterialTheme.theme.colors.primaryUi05
        else -> MaterialTheme.theme.colors.primaryUi01
    }

    Box(
        modifier = Modifier
            .then(if (elevation > 0.dp) Modifier.shadow(elevation, tooltipShape) else Modifier)
            .clip(tooltipShape)
            .background(backgroundColor)
            .then(modifier),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            if (anchorPosition.isTopAligned()) {
                Spacer(
                    modifier = Modifier.height(AnchorHeight),
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
            if (anchorPosition.isBottomAligned()) {
                Spacer(
                    modifier = Modifier.height(AnchorHeight),
                )
            }
        }
    }
}

enum class AnchorPosition {
    TopStart,
    Top,
    TopEnd,
    BottomStart,
    Bottom,
    BottomEnd,
    ;

    internal fun isTopAligned() = this == TopStart || this == Top || this == TopEnd

    internal fun isBottomAligned() = !isTopAligned()
}

private class Tooltip2Shape(
    private val anchorPosition: AnchorPosition,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val cardPath = roundedRectPath(size, density)
        val anchorPath = anchorPath(size, layoutDirection, density)
        val path = Path.combine(PathOperation.Union, cardPath, anchorPath)

        return Outline.Generic(path)
    }

    private fun roundedRectPath(
        size: Size,
        density: Density,
    ): Path {
        val anchorHeight = density.run { AnchorHeight.toPx() }
        val cornerRadius = density.run { CornerRadius.toPx() }
        val topOffset = if (anchorPosition.isTopAligned()) anchorHeight else 0f
        val bottomOffset = if (anchorPosition.isBottomAligned()) anchorHeight else 0f

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

    private fun anchorPath(
        shapeSize: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ) = when (anchorPosition) {
        AnchorPosition.Top -> topAnchor(shapeSize, density)
        AnchorPosition.Bottom -> bottomAnchor(shapeSize, density)
        AnchorPosition.TopStart -> when (layoutDirection) {
            LayoutDirection.Ltr -> topLeftAnchor(density)
            LayoutDirection.Rtl -> topRightAnchor(shapeSize, density)
        }

        AnchorPosition.TopEnd -> when (layoutDirection) {
            LayoutDirection.Ltr -> topRightAnchor(shapeSize, density)
            LayoutDirection.Rtl -> topLeftAnchor(density)
        }

        AnchorPosition.BottomStart -> when (layoutDirection) {
            LayoutDirection.Ltr -> bottomLeftAnchor(shapeSize, density)
            LayoutDirection.Rtl -> bottomRightAnchor(shapeSize, density)
        }

        AnchorPosition.BottomEnd -> when (layoutDirection) {
            LayoutDirection.Ltr -> bottomRightAnchor(shapeSize, density)
            LayoutDirection.Rtl -> bottomLeftAnchor(shapeSize, density)
        }
    }

    private fun topAnchor(
        shapeSize: Size,
        density: Density,
    ) = Path().apply {
        val scale = density.density
        val anchorHeight = density.run { AnchorHeight.toPx() }
        val anchorWidth = density.run { CenterAnchorWidth.toPx() }

        moveTo(shapeSize.width / 2 - anchorWidth / 2, anchorHeight)
        addEdgeAnchor(scale)
    }

    private fun bottomAnchor(
        shapeSize: Size,
        density: Density,
    ) = Path().apply {
        val scale = density.density
        val anchorHeight = density.run { AnchorHeight.toPx() }
        val anchorWidth = density.run { CenterAnchorWidth.toPx() }

        moveTo(shapeSize.width / 2 - anchorWidth / 2, shapeSize.height - anchorHeight)
        addEdgeAnchor(scale, flipVertically = true)
    }

    private fun topRightAnchor(
        shapeSize: Size,
        density: Density,
    ) = Path().apply {
        val scale = density.density
        val anchorHeight = density.run { AnchorHeight.toPx() }
        val anchorWidth = density.run { CornerAnchorWidth.toPx() }

        moveTo(shapeSize.width - anchorWidth, anchorHeight)
        addCornerAnchor(scale)
    }

    private fun topLeftAnchor(
        density: Density,
    ) = Path().apply {
        val scale = density.density
        val anchorHeight = density.run { AnchorHeight.toPx() }
        val anchorWidth = density.run { CornerAnchorWidth.toPx() }

        moveTo(anchorWidth, anchorHeight)
        addCornerAnchor(scale, flipHorizontally = true)
    }

    private fun bottomRightAnchor(
        shapeSize: Size,
        density: Density,
    ) = Path().apply {
        val scale = density.density
        val anchorHeight = density.run { AnchorHeight.toPx() }
        val anchorWidth = density.run { CornerAnchorWidth.toPx() }

        moveTo(shapeSize.width - anchorWidth, shapeSize.height - anchorHeight)
        addCornerAnchor(scale, flipVertically = true)
    }

    private fun bottomLeftAnchor(
        shapeSize: Size,
        density: Density,
    ) = Path().apply {
        val scale = density.density
        val anchorHeight = density.run { AnchorHeight.toPx() }
        val anchorWidth = density.run { CornerAnchorWidth.toPx() }

        moveTo(anchorWidth, shapeSize.height - anchorHeight)
        addCornerAnchor(scale, flipHorizontally = true, flipVertically = true)
    }

    private fun Path.addEdgeAnchor(
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

    private fun Path.addCornerAnchor(
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

private val AnchorHeight = 13.dp
private val CornerAnchorWidth = 40.dp
private val CenterAnchorWidth = 47.dp
private val CornerRadius = 10.dp

@Preview
@Composable
private fun Tooltip2ThemePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        Box(
            modifier = Modifier.padding(16.dp),
        ) {
            Tooltip2(
                title = "Sort by “Recently Played”",
                body = "You can now sort by Recently Played and quickly pick up where you left off.",
                anchorPosition = AnchorPosition.BottomStart,
                modifier = Modifier.widthIn(max = 300.dp),
            )
        }
    }
}

@Preview
@Composable
private fun Tooltip2AnchorPreview(
    @PreviewParameter(AnchorPositionParameterProvider::class) anchorPosition: AnchorPosition,
) {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        Box(
            modifier = Modifier.padding(16.dp),
        ) {
            Tooltip2(
                title = "Sort by “Recently Played”",
                body = "You can now sort by Recently Played and quickly pick up where you left off.",
                anchorPosition = anchorPosition,
                modifier = Modifier.widthIn(max = 300.dp),
            )
        }
    }
}

private class AnchorPositionParameterProvider : PreviewParameterProvider<AnchorPosition> {
    override val values = AnchorPosition.entries.asSequence()
}
