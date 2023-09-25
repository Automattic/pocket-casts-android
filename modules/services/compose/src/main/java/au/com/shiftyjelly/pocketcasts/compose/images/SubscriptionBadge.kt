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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.SubscriptionTier
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

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
                SubscriptionBadgeDisplayMode.Black -> colorResource(UR.color.plus_gold)
                SubscriptionBadgeDisplayMode.Colored -> Color.White
            },
            backgroundColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> Color.Black
                SubscriptionBadgeDisplayMode.Colored -> colorResource(UR.color.plus_gold)
            },
            textColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> colorResource(UR.color.plus_gold)
                SubscriptionBadgeDisplayMode.Colored -> Color.White
            },
        )
        SubscriptionTier.PATRON -> SubscriptionBadge(
            iconRes = IR.drawable.ic_patron,
            shortNameRes = LR.string.pocket_casts_patron_short,
            iconColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> colorResource(UR.color.patron_purple_light)
                SubscriptionBadgeDisplayMode.Colored -> Color.White
            },
            backgroundColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> Color.Black
                SubscriptionBadgeDisplayMode.Colored -> colorResource(UR.color.patron_purple)
            },
            textColor = when (displayMode) {
                SubscriptionBadgeDisplayMode.Black -> Color.White
                SubscriptionBadgeDisplayMode.Colored -> Color.White
            },
        )
        SubscriptionTier.UNKNOWN -> throw IllegalStateException("Unknown subscription tier")
    }
}

enum class SubscriptionBadgeDisplayMode {
    Black,
    Colored,
}

@ShowkaseComposable(name = "SubscriptionBadge", group = "Images", styleName = "Plus - Colored", defaultStyle = true)
@Preview(name = "Colored")
@Composable
fun SubscriptionBadgePlusColoredPreview() {
    SubscriptionBadgeForTier(
        tier = SubscriptionTier.PLUS,
        displayMode = SubscriptionBadgeDisplayMode.Colored
    )
}

@ShowkaseComposable(name = "SubscriptionBadge", group = "Images", styleName = "Plus - Black")
@Preview(name = "Black")
@Composable
fun SubscriptionBadgePlusBlackPreview() {
    SubscriptionBadgeForTier(
        tier = SubscriptionTier.PLUS,
        displayMode = SubscriptionBadgeDisplayMode.Black
    )
}

@ShowkaseComposable(name = "SubscriptionBadge", group = "Images", styleName = "Patron - Colored")
@Preview(name = "Colored")
@Composable
fun SubscriptionBadgePatronColoredPreview() {
    SubscriptionBadgeForTier(
        tier = SubscriptionTier.PATRON,
        displayMode = SubscriptionBadgeDisplayMode.Colored
    )
}

@ShowkaseComposable(name = "SubscriptionBadge", group = "Images", styleName = "Patron - Black")
@Preview(name = "Black")
@Composable
fun SubscriptionBadgePatronBlackPreview() {
    SubscriptionBadgeForTier(
        tier = SubscriptionTier.PATRON,
        displayMode = SubscriptionBadgeDisplayMode.Black
    )
}
