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
import au.com.shiftyjelly.pocketcasts.compose.patronPurple
import au.com.shiftyjelly.pocketcasts.compose.patronPurpleDark
import au.com.shiftyjelly.pocketcasts.compose.patronPurpleLight
import au.com.shiftyjelly.pocketcasts.compose.plusGold
import au.com.shiftyjelly.pocketcasts.compose.plusGoldDark
import au.com.shiftyjelly.pocketcasts.compose.plusGoldLight
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
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
        SubscriptionTier.Plus -> SubscriptionBadge(
            fontSize = fontSize,
            padding = padding,
            iconRes = IR.drawable.ic_plus,
            shortNameRes = LR.string.pocket_casts_plus_short,
            iconColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> Color.plusGold
                SubscriptionBadgeDisplayMode.Colored,
                SubscriptionBadgeDisplayMode.ColoredDark,
                -> MaterialTheme.theme.colors.primaryUi01
                SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground -> Color.White
                SubscriptionBadgeDisplayMode.ColoredWithBlackForeground -> Color.Black
            },
            backgroundColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> Color.Black
                SubscriptionBadgeDisplayMode.ColoredDark -> Color.plusGoldDark
                SubscriptionBadgeDisplayMode.Colored,
                SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground,
                SubscriptionBadgeDisplayMode.ColoredWithBlackForeground,
                -> Color.plusGold
            },
            textColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> Color.White
                SubscriptionBadgeDisplayMode.Colored,
                SubscriptionBadgeDisplayMode.ColoredDark,
                -> MaterialTheme.theme.colors.primaryUi01
                SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground -> Color.White
                SubscriptionBadgeDisplayMode.ColoredWithBlackForeground -> Color.Black
            },
            iconSize = iconSize,
            modifier = modifier,
        )

        SubscriptionTier.Patron -> SubscriptionBadge(
            fontSize = fontSize,
            padding = padding,
            iconRes = IR.drawable.ic_patron,
            shortNameRes = LR.string.pocket_casts_patron_short,
            iconColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> Color.patronPurpleLight
                SubscriptionBadgeDisplayMode.Colored,
                SubscriptionBadgeDisplayMode.ColoredDark,
                SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground,
                -> Color.White
                SubscriptionBadgeDisplayMode.ColoredWithBlackForeground -> Color.Black
            },
            backgroundColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> Color.Black
                SubscriptionBadgeDisplayMode.ColoredDark -> Color.patronPurpleDark
                SubscriptionBadgeDisplayMode.Colored,
                SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground,
                SubscriptionBadgeDisplayMode.ColoredWithBlackForeground,
                -> Color.patronPurple
            },
            textColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> Color.White
                SubscriptionBadgeDisplayMode.Colored,
                SubscriptionBadgeDisplayMode.ColoredDark,
                SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground,
                -> Color.White
                SubscriptionBadgeDisplayMode.ColoredWithBlackForeground -> Color.Black
            },
            iconSize = iconSize,
            modifier = modifier,
        )
    }
}

@Composable
fun SubscriptionIconForTier(
    tier: SubscriptionTier,
    modifier: Modifier = Modifier,
    iconSize: Dp = 16.dp,
) {
    when (tier) {
        SubscriptionTier.Plus -> Icon(
            painter = painterResource(IR.drawable.ic_plus),
            contentDescription = stringResource(LR.string.pocket_casts_plus_short),
            tint = Color.plusGold,
            modifier = modifier
                .size(iconSize),
        )

        SubscriptionTier.Patron -> Icon(
            painter = painterResource(IR.drawable.ic_patron),
            contentDescription = stringResource(LR.string.pocket_casts_patron_short),
            tint = if (MaterialTheme.theme.isLight) {
                Color.patronPurple
            } else {
                Color.patronPurpleLight
            },
            modifier = modifier
                .size(iconSize),
        )
    }
}

@Composable
fun OfferBadge(
    text: String,
    textColor: Int,
    backgroundColor: Int,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    padding: Dp = 4.dp,
) {
    Card(
        shape = RoundedCornerShape(percent = 50),
        backgroundColor = colorResource(id = backgroundColor),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
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

@Preview(name = "Colored")
@Composable
private fun SubscriptionBadgePlusColoredLightThemePreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        SubscriptionBadgeForTier(
            tier = SubscriptionTier.Plus,
            displayMode = SubscriptionBadgeDisplayMode.Colored,
        )
    }
}

@Preview(name = "Colored")
@Composable
private fun SubscriptionBadgePlusColoredDarkThemePreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        SubscriptionBadgeForTier(
            tier = SubscriptionTier.Plus,
            displayMode = SubscriptionBadgeDisplayMode.Colored,
        )
    }
}

@Preview(name = "Colored")
@Composable
private fun SubscriptionBadgePlusColoredDarkLightThemePreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        SubscriptionBadgeForTier(
            tier = SubscriptionTier.Plus,
            displayMode = SubscriptionBadgeDisplayMode.ColoredDark,
        )
    }
}

@Preview(name = "ColoredDark")
@Composable
private fun SubscriptionBadgePlusColoredDarkDarkThemePreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        SubscriptionBadgeForTier(
            tier = SubscriptionTier.Plus,
            displayMode = SubscriptionBadgeDisplayMode.ColoredDark,
        )
    }
}

@Preview(name = "ColoredWithWhiteForeground")
@Composable
private fun SubscriptionBadgePlusColoredWhiteForegroundPreview() {
    SubscriptionBadgeForTier(
        tier = SubscriptionTier.Plus,
        displayMode = SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground,
    )
}

@Preview(name = "Black")
@Composable
private fun SubscriptionBadgePlusBlackPreview() {
    SubscriptionBadgeForTier(
        tier = SubscriptionTier.Plus,
        displayMode = SubscriptionBadgeDisplayMode.Black,
    )
}

@Preview(name = "ColoredWithBlackForeground")
@Composable
private fun SubscriptionBadgePlusColoredWithBlackForegroundPreview() {
    SubscriptionBadgeForTier(
        tier = SubscriptionTier.Plus,
        displayMode = SubscriptionBadgeDisplayMode.ColoredWithBlackForeground,
    )
}

@Preview(name = "Colored")
@Composable
private fun SubscriptionBadgePatronColoredLightThemePreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        SubscriptionBadgeForTier(
            tier = SubscriptionTier.Patron,
            displayMode = SubscriptionBadgeDisplayMode.Colored,
        )
    }
}

@Preview(name = "Colored")
@Composable
private fun SubscriptionBadgePatronColoredDarkThemePreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        SubscriptionBadgeForTier(
            tier = SubscriptionTier.Patron,
            displayMode = SubscriptionBadgeDisplayMode.Colored,
        )
    }
}

@Preview(name = "ColoredDark")
@Composable
private fun SubscriptionBadgePatronColoredDarkLightThemePreview() {
    AppThemeWithBackground(Theme.ThemeType.LIGHT) {
        SubscriptionBadgeForTier(
            tier = SubscriptionTier.Patron,
            displayMode = SubscriptionBadgeDisplayMode.ColoredDark,
        )
    }
}

@Preview(name = "ColoredDark")
@Composable
private fun SubscriptionBadgePatronColoredDarkDarkThemePreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        SubscriptionBadgeForTier(
            tier = SubscriptionTier.Patron,
            displayMode = SubscriptionBadgeDisplayMode.ColoredDark,
        )
    }
}

@Preview(name = "ColoredWithWhiteForeground")
@Composable
private fun SubscriptionBadgePatronColoredWhiteForegroundPreview() {
    SubscriptionBadgeForTier(
        tier = SubscriptionTier.Patron,
        displayMode = SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground,
    )
}

@Preview(name = "Black")
@Composable
private fun SubscriptionBadgePatronBlackPreview() {
    SubscriptionBadgeForTier(
        tier = SubscriptionTier.Patron,
        displayMode = SubscriptionBadgeDisplayMode.Black,
    )
}

@Preview(name = "ColoredWithBlackForeground")
@Composable
private fun SubscriptionBadgePatronColoredWithBlackForegroundPreview() {
    SubscriptionBadgeForTier(
        tier = SubscriptionTier.Patron,
        displayMode = SubscriptionBadgeDisplayMode.ColoredWithBlackForeground,
    )
}

@Preview(name = "ColoredWithBlackForegroundAndGradientBackground")
@Composable
private fun SubscriptionBadgePlusWithGradientBackgroundPreview() {
    SubscriptionBadge(
        fontSize = 16.sp,
        padding = 4.dp,
        iconRes = IR.drawable.ic_plus,
        shortNameRes = LR.string.pocket_casts_plus_short,
        iconColor = Color.Black,
        backgroundBrush = Brush.horizontalGradient(
            0f to Color.plusGoldLight,
            1f to Color.plusGoldDark,
        ),
        textColor = Color.Black,
    )
}
