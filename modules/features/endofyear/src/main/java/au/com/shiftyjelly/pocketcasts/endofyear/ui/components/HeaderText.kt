package au.com.shiftyjelly.pocketcasts.endofyear.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadgeDisplayMode
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionBadgeForTier
import au.com.shiftyjelly.pocketcasts.endofyear.ui.EndOfYearMeasurements
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier

@Composable
internal fun HeaderText(
    title: String,
    subtitle: String,
    subscriptionTier: SubscriptionTier?,
    measurements: EndOfYearMeasurements,
    modifier: Modifier = Modifier,
    titleMaxLines: Int = 1,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(horizontal = 24.dp)
            .padding(top = measurements.closeButtonBottomEdge + 24.dp),
    ) {
        if (subscriptionTier != null) {
            SubscriptionBadgeForTier(
                tier = subscriptionTier,
                displayMode = SubscriptionBadgeDisplayMode.Black,
            )
            Spacer(
                modifier = Modifier.height(16.dp),
            )
        }
        BasicText(
            text = title,
            style = TextStyle(
                fontSize = 25.sp,
                fontWeight = FontWeight.W700,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp,
            ),
            color = { Color.White },
            autoSize = TextAutoSize.StepBased(maxFontSize = 25.sp),
            maxLines = titleMaxLines,
        )
        Spacer(
            modifier = Modifier.height(8.dp),
        )
        TextP40(
            text = subtitle,
            disableAutoScale = true,
            color = Color.White,
            fontWeight = FontWeight.W500,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun CompletionRatePreview() {
    BoxWithConstraints {
        val measurements = EndOfYearMeasurements(
            width = maxWidth,
            height = maxHeight,
            statusBarInsets = WindowInsets(top = 16.dp),
            coverFontSize = 260.sp,
            coverTextHeight = 210.dp,
            closeButtonBottomEdge = 52.dp,
        )
        HeaderText(
            title = "Compared to 2024, your listening time skyrocketed 20%",
            subtitle = "Hope you stretched first!",
            subscriptionTier = SubscriptionTier.Plus,
            titleMaxLines = 2,
            measurements = measurements,
            modifier = Modifier
                .background(Color(0xFF27486A))
                .padding(16.dp),
        )
    }
}
