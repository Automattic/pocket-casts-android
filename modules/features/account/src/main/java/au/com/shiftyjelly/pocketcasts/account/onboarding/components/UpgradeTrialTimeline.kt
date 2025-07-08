import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import kotlin.math.max
import au.com.shiftyjelly.pocketcasts.images.R as IR

data class UpgradeTrialItem(
    @DrawableRes val iconResId: Int,
    val title: String,
    val message: String,
)

@Composable
fun UpgradeTrialTimeline(
    items: List<UpgradeTrialItem>,
    modifier: Modifier = Modifier,
    spaceBetweenItems: Dp = 16.dp,
    iconSize: Dp = 43.dp,
    timelineWidth: Dp = 7.dp,
    iconRightPadding: Dp = 14.dp,
) {
    val gradientColors = listOf(
        Color.Transparent,
        MaterialTheme.colors.background.copy(alpha = 0.65f),
    )
    val density = LocalDensity.current
    val iconSizePx = density.run { iconSize.toPx() }
    val timelineWidthPx = density.run { timelineWidth.toPx() }
    val iconPaddingPx = density.run { iconRightPadding.toPx() }
    val iconColor = MaterialTheme.theme.colors.primaryIcon01

    val iconCenterYPositions = mutableListOf<Float>()

    Layout(
        modifier = modifier.drawWithContent {
            if (items.size > 1) {
                drawLine(
                    color = iconColor,
                    strokeWidth = timelineWidthPx,
                    start = Offset(x = iconSizePx / 2, y = iconCenterYPositions.firstOrNull() ?: 0f),
                    end = Offset(x = iconSizePx / 2, y = iconCenterYPositions.lastOrNull() ?: 0f),
                )
            }
            drawContent()
            if (items.size > 1) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = gradientColors,
                    ),
                    topLeft = Offset(x = 0f, y = 0f),
                    size = Size(width = iconSizePx, height = (iconCenterYPositions.lastOrNull() ?: 0f) + iconSizePx),
                )
            }
        },
        content = {
            items.forEach { item ->
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .background(
                            color = iconColor,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(item.iconResId),
                        tint = MaterialTheme.colors.background,
                        contentDescription = "",
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                ) {
                    TextP50(
                        text = item.title,
                        color = MaterialTheme.theme.colors.primaryText01,
                        fontWeight = FontWeight.W700,
                    )
                    TextP50(
                        text = item.message,
                        color = MaterialTheme.theme.colors.primaryText01.copy(alpha = 0.5f),
                    )
                }
            }
        },
    ) { measurables, constraints ->
        val iconPlaceables = mutableListOf<Placeable>()
        val textPlaceables = mutableListOf<Placeable>()

        iconCenterYPositions.clear()

        // restrict amount of vertical and horizontal space each item may take
        val modifiedConstraints = constraints.copy(
            minHeight = iconSizePx.toInt(),
            maxHeight = max(iconSizePx.toInt(), constraints.maxHeight / max(1, items.size)),
            minWidth = iconSizePx.toInt(),
            maxWidth = max(iconSizePx.toInt(), (constraints.maxWidth - iconPaddingPx - iconSizePx).toInt()),
        )

        measurables.chunked(2).forEach { (icon, text) ->
            iconPlaceables.add(icon.measure(modifiedConstraints))
            textPlaceables.add(text.measure(modifiedConstraints))
        }

        // calculate the actual height space this composable requires to fully render itself
        val totalHeight = iconPlaceables.map { it.height }.zip(textPlaceables.map { it.height }) { iconHeight, textHeight ->
            max(iconHeight, textHeight)
        }.sum() + max(0, (spaceBetweenItems.toPx().toInt() * (items.size - 1)))

        layout(constraints.maxWidth, totalHeight) {
            var yPosition = 0f
            val textOffsetX = iconSizePx + iconPaddingPx
            iconPlaceables.zip(textPlaceables) { icon, text ->
                iconCenterYPositions.add((yPosition + icon.height / 2f))
                icon.placeRelative(0, yPosition.toInt())
                text.placeRelative(textOffsetX.toInt(), yPosition.toInt() + ((icon.height - text.height) / 2))
                yPosition += icon.height + spaceBetweenItems.toPx()
            }
        }
    }
}

@Preview
@Composable
private fun PreviewUpgradeTimeline(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(theme) {
        UpgradeTrialTimeline(
            items = listOf(
                UpgradeTrialItem(iconResId = IR.drawable.ic_star, title = "Star", message = "Message"),
                UpgradeTrialItem(iconResId = IR.drawable.ic_envelope, title = "Envelope", message = "Message"),
                UpgradeTrialItem(iconResId = IR.drawable.ic_unlocked, title = "Unlocked", message = "Message"),
            ),
        )
    }
}
