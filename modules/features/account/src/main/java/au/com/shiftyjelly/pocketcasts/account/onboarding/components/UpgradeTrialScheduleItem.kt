package au.com.shiftyjelly.pocketcasts.account.onboarding.components

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

@Composable
fun UpgradeTrialScheduleItem(
    title: String,
    message: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    connection: ScheduleItemConnection? = null,
    iconSize: Dp = 43.dp,
    connectingRodWith: Dp = 8.dp,
) {
    val iconBackground = MaterialTheme.theme.colors.primaryInteractive01
    val density = LocalDensity.current
    val connectingRodWidthPx = density.run { connectingRodWith.toPx() }
    val iconSizePx = density.run { iconSize.toPx() }
    Row(
        modifier = modifier
            .drawWithContent {
                when (connection) {
                    ScheduleItemConnection.TOP -> drawRect(
                        color = iconBackground,
                        topLeft = Offset(x = iconSizePx / 2f - (connectingRodWidthPx / 2f), y = -1f),
                        size = Size(width = connectingRodWidthPx, height = size.height / 2f),
                    )

                    ScheduleItemConnection.BOTTOM -> drawRect(
                        color = iconBackground,
                        topLeft = Offset(x = iconSizePx / 2f - (connectingRodWidthPx / 2f), y = size.height / 2f),
                        size = Size(width = connectingRodWidthPx, height = (size.height / 2f) + 1f),
                    )

                    ScheduleItemConnection.TOP_AND_BOTTOM -> drawRect(
                        color = iconBackground,
                        topLeft = Offset(x = iconSizePx / 2f - (connectingRodWidthPx / 2f), y = 0f),
                        size = Size(width = connectingRodWidthPx, height = size.height),
                    )

                    else -> Unit
                }
                drawContent()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .background(
                    color = iconBackground,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = icon,
                tint = MaterialTheme.colors.background,
                contentDescription = "",
            )
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            TextP50(
                text = title,
                color = MaterialTheme.theme.colors.primaryText01,
                fontWeight = FontWeight.W700,
            )
            TextP50(
                text = message,
                color = MaterialTheme.theme.colors.secondaryText02,
            )
        }
    }
}

enum class ScheduleItemConnection {
    TOP,
    BOTTOM,
    TOP_AND_BOTTOM,
}

@Preview
@Composable
private fun PreviewUpgradeTrialSchedules(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(theme) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            UpgradeTrialScheduleItem(
                title = "Title1",
                message = "Message 1",
                icon = painterResource(IR.drawable.ic_unlocked),
                connection = ScheduleItemConnection.TOP,
            )
        }
    }
}
