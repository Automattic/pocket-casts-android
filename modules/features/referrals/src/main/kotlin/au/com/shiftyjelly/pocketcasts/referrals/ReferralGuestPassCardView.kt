package au.com.shiftyjelly.pocketcasts.referrals

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object ReferralGuestPassCardDefaults {
    val cardAspectRatio = 200f / 315f
    val cardBlur = 100.dp
    val cardRadius = 13.dp
    val cardStrokeColor = Color(0xFF3A3A3A)
}

@Composable
fun ReferralGuestPassCardView(
    modifier: Modifier = Modifier,
) {
    val cardTitle = stringResource(LR.string.referrals_send_guest_pass_card_title)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(ReferralGuestPassCardDefaults.cardRadius))
            .border(
                width = 1.dp,
                color = ReferralGuestPassCardDefaults.cardStrokeColor,
                shape = RoundedCornerShape(ReferralGuestPassCardDefaults.cardRadius),
            )
            .semantics { contentDescription = cardTitle },
    ) {
        ReferralCardAnimatedBackgroundView(
            modifier = Modifier
                .fillMaxSize(),
        )
        TextH60(
            text = cardTitle,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        )

        Image(
            painter = painterResource(IR.drawable.ic_plus),
            contentDescription = null,
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
        )
    }
}

@Preview(device = Devices.PortraitRegular)
@Composable
fun ReferralPassCardViewPreview() {
    ReferralGuestPassCardView(
        modifier = Modifier
            .size(DpSize(315.dp, 200.dp)),
    )
}
