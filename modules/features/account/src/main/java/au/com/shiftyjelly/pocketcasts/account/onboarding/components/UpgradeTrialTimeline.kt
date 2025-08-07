import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import kotlin.math.max
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

data class UpgradeTrialItem(
    @DrawableRes val iconResId: Int,
    val title: String,
    val message: String,
) {
    companion object {
        @Composable
        fun getPreviewItems() = listOf(
            UpgradeTrialItem(
                iconResId = IR.drawable.ic_unlocked,
                title = stringResource(LR.string.onboarding_upgrade_schedule_today),
                message = stringResource(LR.string.onboarding_upgrade_schedule_today_message),
            ),
            UpgradeTrialItem(
                iconResId = IR.drawable.ic_envelope,
                title = stringResource(LR.string.onboarding_upgrade_schedule_day, 24),
                message = stringResource(LR.string.onboarding_upgrade_schedule_notify),
            ),
            UpgradeTrialItem(
                iconResId = IR.drawable.ic_star,
                title = stringResource(LR.string.onboarding_upgrade_schedule_day, 31),
                message = stringResource(LR.string.onboarding_upgrade_schedule_billing, "September 31th"),
            ),
        )
    }
}

@Composable
fun UpgradeTrialTimeline(
    items: List<UpgradeTrialItem>,
    modifier: Modifier = Modifier,
    spaceBetweenItems: Dp = 34.dp,
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
        modifier = modifier
            .semantics {
                collectionInfo = CollectionInfo(
                    rowCount = items.size,
                    columnCount = 0,
                )
            }
            .drawWithContent {
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
            items.forEachIndexed { index, item ->
                Box(
                    modifier = Modifier
                        .focusable(false)
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
                        .focusable()
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .semantics(mergeDescendants = true) {
                            collectionItemInfo = CollectionItemInfo(
                                rowIndex = index,
                                rowSpan = 0,
                                columnIndex = 0,
                                columnSpan = 0,
                            )
                        },
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
            maxHeight = if (constraints.maxHeight == Constraints.Infinity) {
                Constraints.Infinity
            } else {
                max(iconSizePx.toInt(), constraints.maxHeight / max(1, items.size))
            },
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
            var offsetY = 0
            val textOffsetX = iconSizePx + iconPaddingPx
            iconPlaceables.zip(textPlaceables) { icon, text ->
                iconCenterYPositions.add((offsetY + icon.height / 2f))
                icon.placeRelative(0, offsetY)
                text.placeRelative(textOffsetX.toInt(), offsetY + max(0, (icon.height - text.height) / 2))
                offsetY += max(icon.height, text.height) + spaceBetweenItems.toPx().toInt()
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
            items = UpgradeTrialItem.getPreviewItems(),
        )
    }
}
