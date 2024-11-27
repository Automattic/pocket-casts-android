package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
fun SubscriptionBadge(
    subscriptionTier: SubscriptionTier,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Black,
    fontSize: TextUnit = 12.sp,
    fontColor: Color = Color.White,
    iconSize: Dp = 12.dp,
    iconColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
) {
    val (textId, iconId) = when (subscriptionTier) {
        SubscriptionTier.NONE -> return
        SubscriptionTier.PLUS -> LR.string.pocket_casts_plus_short to IR.drawable.ic_plus
        SubscriptionTier.PATRON -> LR.string.pocket_casts_patron_short to IR.drawable.ic_patron
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(50))
            .padding(contentPadding),
    ) {
        Image(
            painter = painterResource(iconId),
            colorFilter = iconColor?.let(ColorFilter::tint),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
        )
        Spacer(
            modifier = Modifier.width(4.dp),
        )
        TextH50(
            text = stringResource(textId),
            color = fontColor,
            fontSize = fontSize,
            lineHeight = fontSize,
        )
    }
}

@Preview
@Composable
private fun BadgesPreview() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .background(Color.White)
            .padding(8.dp),
    ) {
        SubscriptionBadge(
            subscriptionTier = SubscriptionTier.PATRON,
        )
        SubscriptionBadge(
            subscriptionTier = SubscriptionTier.PATRON,
            backgroundColor = colorResource(UR.color.patron_purple),
            fontColor = Color.White,
            iconColor = Color.White,
        )
        SubscriptionBadge(
            subscriptionTier = SubscriptionTier.PLUS,
        )
        SubscriptionBadge(
            subscriptionTier = SubscriptionTier.PLUS,
            backgroundColor = colorResource(UR.color.plus_gold_dark),
            fontColor = Color.Black,
            iconColor = Color.Black,
        )
        SubscriptionBadge(
            subscriptionTier = SubscriptionTier.NONE,
        )
    }
}
