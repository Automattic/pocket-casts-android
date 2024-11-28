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
import au.com.shiftyjelly.pocketcasts.compose.PocketCastsColors
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

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

    Box(
        modifier = modifier
            .background(background, RoundedCornerShape(percent = 50))
            .padding(horizontal = padding * 2, vertical = padding),
    ) {
        SubscriptionBadgeContent(iconRes, iconSize, iconColor, shortNameRes, textColor, fontSize, padding)
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
    Box(
        modifier = modifier
            .background(backgroundBrush, RoundedCornerShape(percent = 50))
            .padding(horizontal = padding * 2, vertical = padding),
    ) {
        SubscriptionBadgeContent(iconRes, iconSize, iconColor, shortNameRes, textColor, fontSize, padding)
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
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    iconSize: Dp = 14.dp,
    padding: Dp = 4.dp,
) {
    when (tier) {
        SubscriptionTier.PLUS -> SubscriptionBadge(
            fontSize = fontSize,
            padding = padding,
            iconRes = IR.drawable.ic_plus,
            shortNameRes = LR.string.pocket_casts_plus_short,
            iconColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> PocketCastsColors.plusGold
                SubscriptionBadgeDisplayMode.Colored,
                SubscriptionBadgeDisplayMode.ColoredDark -> MaterialTheme.theme.colors.primaryUi01
                SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground -> Color.White
                SubscriptionBadgeDisplayMode.ColoredWithBlackForeground -> Color.Black
            },
            backgroundColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> Color.Black
                SubscriptionBadgeDisplayMode.ColoredDark -> PocketCastsColors.plusGoldDark
                SubscriptionBadgeDisplayMode.Colored,
                SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground,
                SubscriptionBadgeDisplayMode.ColoredWithBlackForeground,
                -> PocketCastsColors.plusGold
            },
            textColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> Color.White
                SubscriptionBadgeDisplayMode.Colored,
                SubscriptionBadgeDisplayMode.ColoredDark -> MaterialTheme.theme.colors.primaryUi01
                SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground -> Color.White
                SubscriptionBadgeDisplayMode.ColoredWithBlackForeground -> Color.Black
            },
            iconSize = iconSize,
            modifier = modifier,
        )

        SubscriptionTier.PATRON -> SubscriptionBadge(
            fontSize = fontSize,
            padding = padding,
            iconRes = IR.drawable.ic_patron,
            shortNameRes = LR.string.pocket_casts_patron_short,
            iconColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> PocketCastsColors.patronPurpleLight
                SubscriptionBadgeDisplayMode.Colored,
                SubscriptionBadgeDisplayMode.ColoredDark -> if (MaterialTheme.theme.isLight) Color.White else Color.Black
                SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground -> Color.White
                SubscriptionBadgeDisplayMode.ColoredWithBlackForeground -> Color.Black
            },
            backgroundColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> Color.Black
                SubscriptionBadgeDisplayMode.ColoredDark -> PocketCastsColors.patronPurpleDark
                SubscriptionBadgeDisplayMode.Colored,
                SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground,
                SubscriptionBadgeDisplayMode.ColoredWithBlackForeground,
                -> PocketCastsColors.patronPurple
            },
            textColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> Color.White
                SubscriptionBadgeDisplayMode.Colored,
                SubscriptionBadgeDisplayMode.ColoredDark -> if (MaterialTheme.theme.isLight) Color.White else Color.Black
                SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground -> Color.White
                SubscriptionBadgeDisplayMode.ColoredWithBlackForeground -> Color.Black
            },
            iconSize = iconSize,
            modifier = modifier,
        )

        SubscriptionTier.NONE -> Unit
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
            tint = PocketCastsColors.plusGold,
            modifier = Modifier
                .size(iconSize),
        )

        SubscriptionTier.PATRON -> Icon(
            painter = painterResource(IR.drawable.ic_patron),
            contentDescription = stringResource(LR.string.pocket_casts_patron_short),
            tint = if (MaterialTheme.theme.isLight) {
                PocketCastsColors.patronPurple
            } else {
                PocketCastsColors.patronPurpleLight
            },
            modifier = Modifier
                .size(iconSize),
        )

        SubscriptionTier.NONE -> Unit
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
        shape = RoundedCornerShape(percent = 50),
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
    ColoredDark,
    ColoredWithWhiteForeground,
    ColoredWithBlackForeground,
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

@ShowkaseComposable(name = "SubscriptionBadge", group = "Images", styleName = "Plus - Colored on dark theme")
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

@ShowkaseComposable(name = "SubscriptionBadge", group = "Images", styleName = "Plus - Colored Dark on light theme", defaultStyle = true)
@Preview(name = "Colored")
@Composable
fun SubscriptionBadgePlusColoredDarkLightThemePreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        SubscriptionBadgeForTier(
            tier = SubscriptionTier.PLUS,
            displayMode = SubscriptionBadgeDisplayMode.ColoredDark,
        )
    }
}

@ShowkaseComposable(name = "SubscriptionBadge", group = "Images", styleName = "Plus - Colored Dark on dark theme")
@Preview(name = "Colored")
@Composable
fun SubscriptionBadgePlusColoredDarkDarkThemePreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        SubscriptionBadgeForTier(
            tier = SubscriptionTier.PLUS,
            displayMode = SubscriptionBadgeDisplayMode.ColoredDark,
        )
    }
}

@ShowkaseComposable(name = "SubscriptionBadge", group = "Images", styleName = "Plus - Colored with white foreground")
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

@ShowkaseComposable(name = "SubscriptionBadge", group = "Images", styleName = "Plus - Colored with black foreground")
@Preview(name = "ColoredWithBlackForeground")
@Composable
fun SubscriptionBadgePlusColoredWithBlackForegroundPreview() {
    SubscriptionBadgeForTier(
        tier = SubscriptionTier.PLUS,
        displayMode = SubscriptionBadgeDisplayMode.ColoredWithBlackForeground,
    )
}

@ShowkaseComposable(name = "SubscriptionBadge", group = "Images", styleName = "Patron - Colored on light theme", defaultStyle = true)
@Preview(name = "Colored")
@Composable
fun SubscriptionBadgePatronColoredLightThemePreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        SubscriptionBadgeForTier(
            tier = SubscriptionTier.PATRON,
            displayMode = SubscriptionBadgeDisplayMode.Colored,
        )
    }
}

@ShowkaseComposable(name = "SubscriptionBadge", group = "Images", styleName = "Patron - Colored on dark theme")
@Preview(name = "Colored")
@Composable
fun SubscriptionBadgePatronColoredDarkThemePreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        SubscriptionBadgeForTier(
            tier = SubscriptionTier.PATRON,
            displayMode = SubscriptionBadgeDisplayMode.Colored,
        )
    }
}

@ShowkaseComposable(name = "SubscriptionBadge", group = "Images", styleName = "Patron - Colored Dark on light theme", defaultStyle = true)
@Preview(name = "Colored")
@Composable
fun SubscriptionBadgePatronColoredDarkLightThemePreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        SubscriptionBadgeForTier(
            tier = SubscriptionTier.PATRON,
            displayMode = SubscriptionBadgeDisplayMode.ColoredDark,
        )
    }
}

@ShowkaseComposable(name = "SubscriptionBadge", group = "Images", styleName = "Patron - Colored Dark on dark theme")
@Preview(name = "Colored")
@Composable
fun SubscriptionBadgePatronColoredDarkDarkThemePreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        SubscriptionBadgeForTier(
            tier = SubscriptionTier.PATRON,
            displayMode = SubscriptionBadgeDisplayMode.ColoredDark,
        )
    }
}

@ShowkaseComposable(name = "SubscriptionBadge", group = "Images", styleName = "Patron - Colored with white foreground")
@Preview(name = "Colored")
@Composable
fun SubscriptionBadgePatronColoredWhiteForegroundPreview() {
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
fun SubscriptionBadgePlusWithGradientBackgroundPreview() {
    SubscriptionBadge(
        fontSize = 16.sp,
        padding = 4.dp,
        iconRes = IR.drawable.ic_plus,
        shortNameRes = LR.string.pocket_casts_plus_short,
        iconColor = Color.Black,
        backgroundBrush = Brush.horizontalGradient(
            0f to PocketCastsColors.plusGoldLight,
            1f to PocketCastsColors.plusGoldDark,
        ),
        textColor = Color.Black,
    )
}
