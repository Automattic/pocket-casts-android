package au.com.shiftyjelly.pocketcasts.compose.images

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
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
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.SubscriptionTier
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
    val bgColor = backgroundColor ?: Color.Black
    Card(
        shape = RoundedCornerShape(pillCornerRadiusInDp),
        backgroundColor = backgroundColor ?: bgColor,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = padding * 2, vertical = padding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(iconSize)
                    .background(bgColor),
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
}

@Composable
fun SubscriptionBadgeForTier(
    tier: SubscriptionTier,
    displayMode: SubscriptionBadgeDisplayMode,
) {
    when (tier) {
        SubscriptionTier.PLUS -> SubscriptionBadge(
            iconRes = IR.drawable.ic_plus,
            shortNameRes = LR.string.pocket_casts_plus_short,
            iconColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> SubscriptionTierColor.plusGold
                SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground -> Color.White
                SubscriptionBadgeDisplayMode.ColoredWithBlackForeground -> Color.Black
            },
            backgroundColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> Color.Black
                SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground,
                SubscriptionBadgeDisplayMode.ColoredWithBlackForeground,
                -> SubscriptionTierColor.plusGold
            },
            textColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> SubscriptionTierColor.plusGold
                SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground -> Color.White
                SubscriptionBadgeDisplayMode.ColoredWithBlackForeground -> Color.Black
            },
        )
        SubscriptionTier.PATRON -> SubscriptionBadge(
            iconRes = IR.drawable.ic_patron,
            shortNameRes = LR.string.pocket_casts_patron_short,
            iconColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> SubscriptionTierColor.patronPurpleLight
                SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground -> Color.White
                SubscriptionBadgeDisplayMode.ColoredWithBlackForeground -> Color.Black
            },
            backgroundColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> Color.Black
                SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground,
                SubscriptionBadgeDisplayMode.ColoredWithBlackForeground,
                -> SubscriptionTierColor.patronPurple
            },
            textColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> Color.White
                SubscriptionBadgeDisplayMode.ColoredWithWhiteForeground -> Color.White
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
    ColoredWithWhiteForeground,
    ColoredWithBlackForeground,
}

object SubscriptionTierColor {
    val plusGold = Color(0xFFFFD846)
    val patronPurple = Color(0xFF6046F5)
    val patronPurpleLight = Color(0xFFAFA2FA)
}

@ShowkaseComposable(name = "SubscriptionBadge", group = "Images", styleName = "Plus - Colored", defaultStyle = true)
@Preview(name = "Colored")
@Composable
fun SubscriptionBadgePlusColoredPreview() {
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

@ShowkaseComposable(name = "SubscriptionBadge", group = "Images", styleName = "Patron - Colored")
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
