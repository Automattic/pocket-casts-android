import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.painter.Painter
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
import au.com.shiftyjelly.pocketcasts.images.R as IR

data class UpgradeTrialItem(
    val icon: Painter,
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
) {
    val gradientColors = listOf(
        Color.Transparent,
        MaterialTheme.colors.background.copy(alpha = 0.8f),
    )
    val density = LocalDensity.current
    val iconSizePx = density.run { iconSize.toPx() }
    val timelineWidthPx = density.run { timelineWidth.toPx() }
    val iconColor = MaterialTheme.theme.colors.primaryIcon01
    Column(
        verticalArrangement = Arrangement.spacedBy(spaceBetweenItems),
        modifier = modifier.drawWithContent {
            drawLine(
                color = iconColor,
                strokeWidth = timelineWidthPx,
                start = Offset(x = iconSizePx / 2, y = iconSizePx / 2),
                end = Offset(x = iconSizePx / 2, y = size.height - (iconSizePx / 2f)),
            )
            drawContent()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = gradientColors,
                ),
                topLeft = Offset(x = 0f, y = 0f),
                size = Size(width = iconSizePx, size.height),
            )
        },
    ) {
        items.forEach { item ->
            UpgradeTrialScheduleItem(
                item = item,
                iconSize = iconSize,
                iconBackgroundColor = iconColor,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun UpgradeTrialScheduleItem(
    item: UpgradeTrialItem,
    modifier: Modifier = Modifier,
    iconSize: Dp = 43.dp,
    iconBackgroundColor: Color = MaterialTheme.theme.colors.primaryIcon01,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .background(
                    color = iconBackgroundColor,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = item.icon,
                tint = MaterialTheme.colors.background,
                contentDescription = "",
            )
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            TextP50(
                text = item.title,
                color = MaterialTheme.theme.colors.primaryText01,
                fontWeight = FontWeight.W700,
            )
            TextP50(
                text = item.message,
                color = MaterialTheme.theme.colors.secondaryText02,
            )
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
                UpgradeTrialItem(icon = painterResource(IR.drawable.ic_star), title = "Star", message = "Message"),
                UpgradeTrialItem(icon = painterResource(IR.drawable.ic_envelope), title = "Envelope", message = "Message"),
                UpgradeTrialItem(icon = painterResource(IR.drawable.ic_unlocked), title = "Unlocked", message = "Message"),
            ),
        )
    }
}
