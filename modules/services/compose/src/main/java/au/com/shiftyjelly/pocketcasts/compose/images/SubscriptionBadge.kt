package au.com.shiftyjelly.pocketcasts.compose.images

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val pillCornerRadiusInDp = 800.dp

@Composable
fun SubscriptionBadge(
    @DrawableRes iconRes: Int,
    @StringRes shortNameRes: Int,
    modifier: Modifier = Modifier,
    iconColor: Color = Color.Unspecified,
    iconSize: Dp = 14.dp,
    fontSize: TextUnit = 14.sp,
    padding: Dp = 4.dp,
    backgroundColor: Color? = null,
    textColor: Color? = null,
) {
    val background = backgroundColor ?: Color.Black

    Card(
        shape = RoundedCornerShape(pillCornerRadiusInDp),
        modifier = modifier
            .background(Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .background(background)
                .padding(horizontal = padding * 2, vertical = padding),
        ) {
            SubscriptionBadgeContent(iconRes, iconSize, iconColor, shortNameRes, textColor, fontSize, padding)
        }
    }
}

@Composable
fun SubscriptionBadge(
    @DrawableRes iconRes: Int,
    @StringRes shortNameRes: Int,
    backgroundBrush: Brush,
    modifier: Modifier = Modifier,
    iconColor: Color = Color.Unspecified,
    iconSize: Dp = 14.dp,
    fontSize: TextUnit = 14.sp,
    padding: Dp = 4.dp,
    textColor: Color? = null,
) {
    Card(
        shape = RoundedCornerShape(pillCornerRadiusInDp),
        modifier = modifier
            .background(Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .background(backgroundBrush)
                .padding(horizontal = padding * 2, vertical = padding),
        ) {
            SubscriptionBadgeContent(iconRes, iconSize, iconColor, shortNameRes, textColor, fontSize, padding)
        }
    }
}

@Composable
private fun SubscriptionBadgeContent(
    iconRes: Int,
    iconSize: Dp,
    iconColor: Color,
    shortNameRes: Int,
    textColor: Color?,
    fontSize: TextUnit,
    padding: Dp,
) {
    Row(
        modifier = Modifier
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier
                .size(iconSize),
            tint = iconColor,
        )
        TextH50(
            text = stringResource(shortNameRes),
            color = textColor ?: Color.White,
            fontSize = fontSize,
            lineHeight = fontSize,
            modifier = Modifier
                .padding(start = padding),
        )
    }
}

@Composable
fun SubscriptionBadgeForTier(
    tier: SubscriptionTier,
    displayMode: SubscriptionBadgeDisplayMode,
    fontSize: TextUnit = 14.sp,
    padding: Dp = 4.dp,
) {
    when (tier) {
        SubscriptionTier.PLUS -> SubscriptionBadge(
            fontSize = fontSize,
            padding = padding,
            iconRes = IR.drawable.ic_plus,
            shortNameRes = LR.string.pocket_casts_plus_short,
            iconColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> SubscriptionTierColor.plusGold
                SubscriptionBadgeDisplayMode.Colored -> MaterialTheme.theme.colors.primaryUi01
                SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground -> Color.White
                SubscriptionBadgeDisplayMode.ColoredWithBlackForeground -> Color.Black
            },
            backgroundColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> Color.Black
                SubscriptionBadgeDisplayMode.Colored,
                SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground,
                SubscriptionBadgeDisplayMode.ColoredWithBlackForeground,
                -> SubscriptionTierColor.plusGold
            },
            textColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> SubscriptionTierColor.plusGold
                SubscriptionBadgeDisplayMode.Colored -> MaterialTheme.theme.colors.primaryUi01
                SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground -> Color.White
                SubscriptionBadgeDisplayMode.ColoredWithBlackForeground -> Color.Black
            },
        )

        SubscriptionTier.PATRON -> SubscriptionBadge(
            fontSize = fontSize,
            padding = padding,
            iconRes = IR.drawable.ic_patron,
            shortNameRes = LR.string.pocket_casts_patron_short,
            iconColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> SubscriptionTierColor.patronPurpleLight
                SubscriptionBadgeDisplayMode.Colored,
                SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground,
                -> Color.White
                SubscriptionBadgeDisplayMode.ColoredWithBlackForeground -> Color.Black
            },
            backgroundColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> Color.Black
                SubscriptionBadgeDisplayMode.Colored,
                SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground,
                SubscriptionBadgeDisplayMode.ColoredWithBlackForeground,
                -> SubscriptionTierColor.patronPurple
            },
            textColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> Color.White
                SubscriptionBadgeDisplayMode.Colored,
                SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground,
                -> Color.White
                SubscriptionBadgeDisplayMode.ColoredWithBlackForeground -> Color.Black
            },
        )

        SubscriptionTier.UNKNOWN -> Unit
    }
}

@Composable
fun SubscriptionIconForTier(
    tier: SubscriptionTier,
    iconSize: Dp = 16.dp,
) {
    when (tier) {
        SubscriptionTier.PLUS -> Icon(
            painter = painterResource(IR.drawable.ic_plus),
            contentDescription = stringResource(LR.string.pocket_casts_plus_short),
            tint = SubscriptionTierColor.plusGold,
            modifier = Modifier
                .size(iconSize),
        )

        SubscriptionTier.PATRON -> Icon(
            painter = painterResource(IR.drawable.ic_patron),
            contentDescription = stringResource(LR.string.pocket_casts_patron_short),
            tint = if (MaterialTheme.theme.isLight) {
                SubscriptionTierColor.patronPurple
            } else {
                SubscriptionTierColor.patronPurpleLight
            },
            modifier = Modifier
                .size(iconSize),
        )

        SubscriptionTier.UNKNOWN -> Unit
    }
}

@Composable
fun OfferBadge(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    padding: Dp = 4.dp,
    backgroundColor: Int,
    textColor: Int,
) {
    Card(
        shape = RoundedCornerShape(pillCornerRadiusInDp),
        backgroundColor = colorResource(id = backgroundColor),
        modifier = modifier,
    ) {
        Row(
            modifier = modifier
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = padding * 2, vertical = padding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextH50(
                textAlign = TextAlign.Center,
                text = text.uppercase(),
                color = colorResource(id = textColor),
                fontSize = fontSize,
                lineHeight = fontSize,
                modifier = Modifier
                    .padding(start = padding),
            )
        }
    }
}

enum class SubscriptionBadgeDisplayMode {
    Black,
    Colored,
    ColoredWithWhiteForeground,
    ColoredWithBlackForeground,
}

object SubscriptionTierColor {
    val plusGold = Color(0xFFFFD846)
    val patronPurple = Color(0xFF6046F5)
    val patronPurpleLight = Color(0xFFAFA2FA)
}

@ShowkaseComposable(name = "SubscriptionBadge", group = "Images", styleName = "Plus - Colored on light theme", defaultStyle = true)
@Preview(name = "Colored")
@Composable
fun SubscriptionBadgePlusColoredLightThemePreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        SubscriptionBadgeForTier(
            tier = SubscriptionTier.PLUS,
            displayMode = SubscriptionBadgeDisplayMode.Colored,
        )
    }
}

@ShowkaseComposable(name = "SubscriptionBadge", group = "Images", styleName = "Plus - Colored on dark theme", defaultStyle = true)
@Preview(name = "Colored")
@Composable
fun SubscriptionBadgePlusColoredDarkThemePreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        SubscriptionBadgeForTier(
            tier = SubscriptionTier.PLUS,
            displayMode = SubscriptionBadgeDisplayMode.Colored,
        )
    }
}

@ShowkaseComposable(name = "SubscriptionBadge", group = "Images", styleName = "Plus - Colored with white foreground", defaultStyle = true)
@Preview(name = "Colored")
@Composable
fun SubscriptionBadgePlusColoredWhiteForegroundPreview() {
    SubscriptionBadgeForTier(
        tier = SubscriptionTier.PLUS,
        displayMode = SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground,
    )
}

@ShowkaseComposable(name = "SubscriptionBadge", group = "Images", styleName = "Plus - Black")
@Preview(name = "Black")
@Composable
fun SubscriptionBadgePlusBlackPreview() {
    SubscriptionBadgeForTier(
        tier = SubscriptionTier.PLUS,
        displayMode = SubscriptionBadgeDisplayMode.Black,
    )
}

@ShowkaseComposable(name = "SubscriptionBadge", group = "Images", styleName = "Patron - Colored with white foreground")
@Preview(name = "Colored")
@Composable
fun SubscriptionBadgePatronColoredPreview() {
    SubscriptionBadgeForTier(
        tier = SubscriptionTier.PATRON,
        displayMode = SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground,
    )
}

@ShowkaseComposable(name = "SubscriptionBadge", group = "Images", styleName = "Patron - Black")
@Preview(name = "Black")
@Composable
fun SubscriptionBadgePatronBlackPreview() {
    SubscriptionBadgeForTier(
        tier = SubscriptionTier.PATRON,
        displayMode = SubscriptionBadgeDisplayMode.Black,
    )
}

@ShowkaseComposable(name = "SubscriptionBadge", group = "Images", styleName = "Plus - Colored with black foreground")
@Preview(name = "ColoredWithBlackForeground")
@Composable
fun SubscriptionBadgePlusColoredWithBlackForegroundPreview() {
    SubscriptionBadgeForTier(
        tier = SubscriptionTier.PLUS,
        displayMode = SubscriptionBadgeDisplayMode.ColoredWithBlackForeground,
    )
}

@ShowkaseComposable(name = "SubscriptionBadge", group = "Images", styleName = "Patron - Colored with black foreground")
@Preview(name = "ColoredWithBlackForeground")
@Composable
fun SubscriptionBadgePatronColoredWithBlackForegroundPreview() {
    SubscriptionBadgeForTier(
        tier = SubscriptionTier.PATRON,
        displayMode = SubscriptionBadgeDisplayMode.ColoredWithBlackForeground,
    )
}

@ShowkaseComposable(name = "SubscriptionBadge", group = "Images", styleName = "Plus - Colored with gradient background")
@Preview(name = "ColoredWithBlackForegroundAndGradientBackground")
@Composable
fun SubscriptionBadgeWithGradientBackgroundPreview() {
    SubscriptionBadge(
        fontSize = 16.sp,
        padding = 4.dp,
        iconRes = R.drawable.ic_plus,
        shortNameRes = LR.string.pocket_casts_plus_short,
        iconColor = Color.Black,
        backgroundBrush = Brush.horizontalGradient(
            0f to Color(0xFFFED745),
            1f to Color(0xFFFEB525),
        ),
        textColor = Color.Black,
    )
}
