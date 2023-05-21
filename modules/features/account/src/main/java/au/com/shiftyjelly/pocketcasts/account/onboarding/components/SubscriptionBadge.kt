package au.com.shiftyjelly.pocketcasts.account.onboarding.components

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.images.R
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

@Preview
@Composable
private fun SubscriptionBadgePreview() {
    SubscriptionBadge(
        iconRes = R.drawable.ic_patron,
        shortNameRes = LR.string.pocket_casts_patron_short,
        backgroundColor = Color.Black,
    )
}
