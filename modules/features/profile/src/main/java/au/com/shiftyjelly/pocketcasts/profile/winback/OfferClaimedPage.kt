package au.com.shiftyjelly.pocketcasts.profile.winback

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.SparkleImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextP30
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun OfferClaimedPage(
    billingCycle: BillingCycle,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(rememberNestedScrollInteropConnection())
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(
            modifier = Modifier.height(52.dp),
        )
        BoxWithConstraints {
            SparkleImage(
                modifier = Modifier.size((maxWidth * 0.4f).coerceAtMost(162.dp)),
            )
        }
        Spacer(
            modifier = Modifier.height(20.dp),
        )
        Text(
            text = when (billingCycle) {
                BillingCycle.Monthly -> stringResource(LR.string.winback_claimed_offer_message_1)
                BillingCycle.Yearly -> stringResource(LR.string.winback_claimed_offer_message_3)
            },
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 33.5.sp,
            color = MaterialTheme.theme.colors.primaryText01,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        TextP30(
            text = when (billingCycle) {
                BillingCycle.Monthly -> stringResource(LR.string.winback_claimed_offer_message_2)
                BillingCycle.Yearly -> stringResource(LR.string.winback_claimed_offer_message_4)
            },
            color = MaterialTheme.theme.colors.primaryText02,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(
            modifier = Modifier.weight(1f),
        )
        RowButton(
            text = stringResource(LR.string.done),
            onClick = onConfirm,
        )
        Spacer(
            modifier = Modifier.height(48.dp),
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun WinbackOfferPageBillingPeriodPreview(
    @PreviewParameter(BillingPeriodParameterProvider::class) billingPeriod: BillingCycle,
) {
    AppThemeWithBackground(
        themeType = ThemeType.ROSE,
    ) {
        OfferClaimedPage(
            billingCycle = billingPeriod,
            onConfirm = {},
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun WinbackOfferPageThemePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(
        themeType = theme,
    ) {
        OfferClaimedPage(
            billingCycle = BillingCycle.Monthly,
            onConfirm = {},
        )
    }
}

private class BillingPeriodParameterProvider : PreviewParameterProvider<BillingCycle> {
    override val values = sequenceOf(
        BillingCycle.Monthly,
        BillingCycle.Yearly,
    )
}
