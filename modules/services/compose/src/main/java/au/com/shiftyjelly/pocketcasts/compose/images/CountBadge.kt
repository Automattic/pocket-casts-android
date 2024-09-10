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
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
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
                color = style.backgroundColor(),
                borderColor = style.borderColor(),
                borderWidth = borderWidthPx.toFloat(),
            ),
        contentAlignment = Alignment.Center,
    ) {
        val text = count.toString()
        val textColor = style.textColor()
        when (style) {
            is CountBadgeStyle.Small -> return
            is CountBadgeStyle.Medium -> TextH60(
                text = text,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.W700,
                disableScale = true,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            is CountBadgeStyle.Big -> TextH50(
                text = text,
                color = textColor,
                disableScale = true,
            )

            is CountBadgeStyle.Custom -> TextH70(
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

    @Composable
    open fun backgroundColor() = MaterialTheme.theme.colors.primaryInteractive01

    @Composable
    open fun borderColor(): Color = if (FeatureFlag.isEnabled(Feature.UPNEXT_IN_TAB_BAR)) {
        MaterialTheme.theme.colors.primaryUi01
    } else {
        MaterialTheme.theme.colors.primaryUi04
    }

    @Composable
    open fun textColor() = MaterialTheme.theme.colors.primaryInteractive02

    data object Small : CountBadgeStyle() {
        override val size = 12.dp
        override val borderWidth = 3.dp
    }

    data object Medium : CountBadgeStyle() {
        override val size = 19.dp
        override val borderWidth = 3.dp
    }

    data object Big : CountBadgeStyle() {
        override val size = 28.dp
        override val borderWidth = 0.dp
    }

    data class Custom(
        override val size: Dp,
        override val borderWidth: Dp,
        val backgroundColor: Color,
        val borderColor: Color,
        val textColor: Color,
    ) : CountBadgeStyle() {
        @Composable
        override fun backgroundColor() = backgroundColor

        @Composable
        override fun borderColor() = borderColor

        @Composable
        override fun textColor() = textColor
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

@ShowkaseComposable(name = "CountBadge", group = "Images", styleName = "Custom")
@Preview(name = "Custom")
@Composable
fun CountBadgeCustomPreview() {
    CountBadgePreview(
        style = CountBadgeStyle.Custom(
            backgroundColor = MaterialTheme.theme.colors.primaryIcon01,
            borderColor = Color.Transparent,
            borderWidth = 0.dp,
            size = 16.dp,
            textColor = MaterialTheme.theme.colors.primaryUi01,
        ),
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
