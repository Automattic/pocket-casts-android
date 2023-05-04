package au.com.shiftyjelly.pocketcasts.account.onboarding.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.images.R

private val iconSizeInDp = 14.dp
private val pillCornerRadiusInDp = 800.dp

@Composable
fun SubscriptionTierPill(
    @DrawableRes iconRes: Int,
    @StringRes shortNameRes: Int,
    modifier: Modifier = Modifier,
    iconColor: Color = Color.Unspecified,
    backgroundColor: Color = Color.Black,
) {
    Card(
        shape = RoundedCornerShape(pillCornerRadiusInDp),
        backgroundColor = backgroundColor,
    ) {
        Row(
            modifier = Modifier
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = modifier
                    .size(iconSizeInDp)
                    .padding(end = 4.dp),
                tint = iconColor,
            )
            TextH50(
                text = stringResource(shortNameRes),
                color = Color.White,
            )
        }
    }
}

@Preview
@Composable
private fun SubscriptionTierPillPreview() {
    SubscriptionTierPill(
        iconRes = R.drawable.ic_patron,
        shortNameRes = au.com.shiftyjelly.pocketcasts.localization.R.string.pocket_casts_patron_short,
        backgroundColor = Color.Black,
    )
}
