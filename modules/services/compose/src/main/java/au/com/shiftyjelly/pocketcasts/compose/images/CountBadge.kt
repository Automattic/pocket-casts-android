package au.com.shiftyjelly.pocketcasts.compose.images

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import com.airbnb.android.showkase.annotation.ShowkaseComposable

@Composable
fun CountBadge(
    count: Int,
    style: CountBadgeStyle,
    modifier: Modifier = Modifier,
) {
    if (count == 0) return
    val minSize = style.size + style.borderWidth * 2
    val borderWidthPx = style.borderWidth.value.toInt().dpToPx(LocalContext.current)
    Box(
        modifier = modifier
            .defaultMinSize(minSize, minSize)
            .badgeBackground(
                color = MaterialTheme.theme.colors.primaryInteractive01,
                borderColor = MaterialTheme.theme.colors.primaryUi04,
                borderWidth = borderWidthPx.toFloat(),
            ),
        contentAlignment = Alignment.Center,
    ) {
        val text = count.toString()
        val textColor = MaterialTheme.theme.colors.primaryInteractive02
        when (style) {
            is CountBadgeStyle.Small -> return
            is CountBadgeStyle.Medium -> TextH70(
                text = text,
                color = textColor,
                fontWeight = FontWeight.W600,
                disableScale = true,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            is CountBadgeStyle.Big -> TextH50(
                text = text,
                color = textColor,
                disableScale = true,
            )
        }
    }
}

private fun Modifier.badgeBackground(
    color: Color,
    borderColor: Color,
    borderWidth: Float,
) = this
    .graphicsLayer { alpha = 0.99f }
    .drawWithContent {
        if (borderWidth > 0) {
            drawRoundRect(
                color = borderColor,
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(size.width / 2),
            )
        }
        drawRoundRect(
            color = color,
            topLeft = Offset(borderWidth, borderWidth),
            size = Size(size.width - borderWidth * 2, size.height - borderWidth * 2),
            cornerRadius = CornerRadius((size.width - borderWidth * 2) / 2),
        )
        drawContent()
    }

sealed class CountBadgeStyle {
    abstract val size: Dp
    abstract val borderWidth: Dp

    data object Small : CountBadgeStyle() {
        override val size = 12.dp
        override val borderWidth = 3.dp
    }

    data object Medium : CountBadgeStyle() {
        override val size = 16.dp
        override val borderWidth = 3.dp
    }

    data object Big : CountBadgeStyle() {
        override val size = 28.dp
        override val borderWidth = 0.dp
    }
}

@ShowkaseComposable(name = "CountBadge", group = "Images", styleName = "Small")
@Preview(name = "Small")
@Composable
fun CountBadgeSmallPreview() {
    CountBadgePreview(
        style = CountBadgeStyle.Small,
    )
}

@ShowkaseComposable(name = "CountBadge", group = "Images", styleName = "Medium")
@Preview(name = "Medium")
@Composable
fun CountBadgeMediumPreview() {
    CountBadgePreview(
        style = CountBadgeStyle.Medium,
    )
}

@ShowkaseComposable(name = "CountBadge", group = "Images", styleName = "Big")
@Preview(name = "Big")
@Composable
fun CountBadgeBigPreview() {
    CountBadgePreview(
        style = CountBadgeStyle.Big,
    )
}

@Composable
fun CountBadgePreview(
    style: CountBadgeStyle,
) {
    AppTheme(Theme.ThemeType.DARK) {
        CountBadge(
            count = 10,
            style = style,
        )
    }
}
